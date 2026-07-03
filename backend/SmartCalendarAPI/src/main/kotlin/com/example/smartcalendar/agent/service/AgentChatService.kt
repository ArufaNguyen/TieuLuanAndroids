package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentAction
import com.example.smartcalendar.agent.dto.AgentChatResponse
import com.example.smartcalendar.agent.dto.AgentToolCallView
import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.agent.dto.NormalizedToolResult
import com.example.smartcalendar.agent.normalizer.ToolResponseNormalizerRegistry
import com.example.smartcalendar.agent.security.SafetyGate
import com.example.smartcalendar.agent.util.DateParamResolver
import com.example.smartcalendar.dto.event.request.CreateEventRequest
import com.example.smartcalendar.dto.tag.request.CreateTagRequest
import com.example.smartcalendar.dto.tag.request.UpdateTagRequest
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.repository.TagRepository
import com.example.smartcalendar.service.AuthService
import com.example.smartcalendar.service.EventService
import com.example.smartcalendar.service.TagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.system.measureTimeMillis

@Service
class AgentChatService(
    private val authService: AuthService,
    private val toolRegistryService: ToolRegistryService,
    private val planner: LlmPlanner,
    private val safetyGate: SafetyGate,
    private val credentialResolver: CredentialResolver,
    private val runnerClient: KnownToolRunnerClient,
    private val normalizers: ToolResponseNormalizerRegistry,
    private val responseComposer: ResponseComposer,
    private val dateParamResolver: DateParamResolver,
    private val loginCredentialPersistService: LoginCredentialPersistService,
    private val eventService: EventService,
    private val tagService: TagService,
    private val tagRepository: TagRepository
) {
    private val log = LoggerFactory.getLogger(AgentChatService::class.java)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    fun chat(sessionToken: String?, message: String, selectedToolId: Int? = null, confirmed: Boolean = false): AgentChatResponse {
        return chatInternal(sessionToken, message, selectedToolId, confirmed)
    }

    fun chatV2(sessionToken: String?, message: String, confirmed: Boolean = false): AgentChatResponse {
        return chatInternal(sessionToken, message, selectedToolId = null, confirmed = confirmed)
    }

    private fun chatInternal(
        sessionToken: String?,
        message: String,
        selectedToolId: Int?,
        confirmed: Boolean
    ): AgentChatResponse {
        if (message.isBlank()) throw ApiException(400, "message must not be blank")
        val session = authService.getValidSession(sessionToken)
            ?: throw ApiException(401, "valid session is required for agent chat")
        val user = session.account?.user
            ?: throw ApiException(401, "valid session is required for agent chat")
        val availableTools = toolRegistryService.getToolsForUser(user.id)
        val requestedTools = selectedToolId?.let { id ->
            val selected = availableTools.firstOrNull { it.id == id }
                ?: throw ApiException(404, "selected tool not found for active user")
            listOf(selected)
        } ?: availableTools
        val tools = if (selectedToolId == null) {
            applyDefaultEventHarness(message, requestedTools)
        } else {
            requestedTools
        }
        if (tools.isEmpty()) {
            return AgentChatResponse("Bạn chưa có API knowledge nào. Hãy upload HAR trước.", needsClarification = true)
        }
        val plan = planner.plan(
            userMessage = message,
            tools = tools,
            currentDate = LocalDate.now(zone),
            timezone = "Asia/Ho_Chi_Minh"
        )
        val validation = safetyGate.validate(plan, tools)
        if (validation.needsClarification) {
            return AgentChatResponse(validation.message ?: "Bạn cần nói rõ hơn yêu cầu.", needsClarification = true)
        }
        if (validation.needsConfirmation && !confirmed) {
            val call = plan.toolCalls.firstOrNull()
            return AgentChatResponse(
                answer = validation.message ?: "Yêu cầu này cần xác nhận trước khi chạy.",
                toolCalls = call?.let { listOf(AgentToolCallView(it.toolId, it.toolName, it.category, it.params)) }.orEmpty(),
                needsConfirmation = true
            )
        }
        if (validation.refused || (plan.action != AgentAction.RUN_TOOL && !(confirmed && validation.needsConfirmation))) {
            return AgentChatResponse(validation.message ?: "Tôi không thể chạy yêu cầu này.")
        }
        val call = plan.toolCalls.single()
        val tool = tools.first { it.id == call.toolId }
        if (tool.method.equals("INTERNAL", ignoreCase = true)) {
            return runInternalTool(user.id, sessionToken.orEmpty(), tool.toolName, call.params, message)
        }
        val credentials = credentialResolver.resolve(
            userId = user.id,
            requiredHeaders = tool.requiredCredentialHeaders,
            optionalHeaders = tool.optionalCredentialHeaders
        )
        if (credentials.missingRequiredHeaders.isNotEmpty()) {
            return AgentChatResponse(
                answer = "Bạn cần đăng nhập portal trước để tôi chạy API này.",
                needsClarification = true,
                missing = credentials.missingRequiredHeaders
            )
        }
        val range = if (tool.category.equals("SCHEDULE", ignoreCase = true)) {
            dateParamResolver.resolveDateRange(message, LocalDate.now(zone))
        } else {
            null
        }
        val runParams = scheduleRunParams(call.params, range)
        val runResults = runParams.map { paramsForRun ->
            runTool(
                userId = user.id,
                sessionToken = sessionToken.orEmpty(),
                toolId = tool.id,
                category = tool.category,
                method = tool.method,
                params = paramsForRun,
                credentials = credentials.headers,
                headers = call.headers,
                body = call.body
            )
        }
        val runResult = runResults.firstOrNull() ?: throw ApiException(500, "known tool runner returned no result")
        val credentialPersisted = runResults.any { loginCredentialPersistService.persistIfLogin(user, tool, it) }
        val normalized = combineNormalized(runResults.map { normalizers.normalize(tool, it) })
        return AgentChatResponse(
            answer = responseComposer.compose(message, tool, plan, runResult, normalized)
                .let { answer -> if (credentialPersisted) "$answer\nĐã lưu token portal để dùng cho các tool tiếp theo." else answer },
            toolCalls = runResults.mapIndexed { index, result ->
                AgentToolCallView(
                    toolId = tool.id,
                    toolName = tool.toolName,
                    category = tool.category,
                    params = runParams.getOrElse(index) { call.params },
                    upstreamStatus = result.status
                )
            }
        )
    }

    private fun applyDefaultEventHarness(message: String, tools: List<AgentToolDescriptor>): List<AgentToolDescriptor> {
        if (!isCalendarEventIntent(message) || wantsPortal(message)) return tools
        val filtered = tools.filterNot { tool ->
            tool.category.equals("SCHEDULE", ignoreCase = true) ||
                tool.category.equals("SCHEDULE_IMPORT", ignoreCase = true)
        }
        return filtered.ifEmpty { tools }
    }

    private fun wantsPortal(message: String): Boolean {
        val text = message.lowercase()
        return "portal" in text
    }

    private fun isCalendarEventIntent(message: String): Boolean {
        val text = message.lowercase()
        return listOf(
            "event",
            "events",
            "calendar",
            "lich",
            "lịch",
            "hom nay",
            "hôm nay",
            "ngay mai",
            "ngày mai",
            "tuan nay",
            "tuần này",
            "mon gi",
            "môn gì"
        ).any(text::contains)
    }

    private fun runInternalTool(
        userId: Int,
        sessionToken: String,
        toolName: String,
        params: Map<String, String>,
        userMessage: String
    ): AgentChatResponse {
        return when (toolName) {
            ToolRegistryService.LIST_EVENTS_TOOL_NAME -> listEventsFromAgent(userId, params)
            ToolRegistryService.CREATE_EVENT_TOOL_NAME -> createEventFromAgent(userId, params, userMessage)
            ToolRegistryService.DELETE_EVENT_TOOL_NAME -> deleteEventFromAgent(userId, params)
            ToolRegistryService.IMPORT_PORTAL_SCHEDULE_TOOL_NAME -> importPortalScheduleFromAgent(userId, sessionToken, params, userMessage)
            ToolRegistryService.CREATE_TAG_TOOL_NAME -> createTagFromAgent(userId, params)
            ToolRegistryService.UPDATE_TAG_TOOL_NAME -> updateTagFromAgent(userId, params, userMessage)
            ToolRegistryService.DELETE_TAG_TOOL_NAME -> deleteTagFromAgent(userId, params)
            else -> throw ApiException(400, "unsupported internal tool: $toolName")
        }
    }

    private fun listEventsFromAgent(userId: Int, params: Map<String, String>): AgentChatResponse {
        val startDate = params["startDate"]?.let(LocalDate::parse)
        val endDate = params["endDate"]?.let(LocalDate::parse)
        val effectiveStart = startDate ?: endDate
        val effectiveEnd = endDate ?: startDate
        if (effectiveStart != null && effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
            throw ApiException(400, "endDate must be on or after startDate")
        }

        val tagId = resolveReadTagId(userId, params)
        val keyword = params["keyword"]?.takeIf(String::isNotBlank)
        val result = eventService.getEvents(
            keyword = keyword,
            tagId = tagId,
            userId = userId,
            from = effectiveStart?.atStartOfDay(),
            to = effectiveEnd?.atTime(LocalTime.MAX)
        )
        if (result.code !in 200..299) throw ApiException(result.code, result.message)
        val events = result.body.orEmpty()
        val rangeText = if (effectiveStart != null && effectiveEnd != null) {
            "trong khoang ${formatDateRange(effectiveStart, effectiveEnd)}"
        } else {
            "hien co"
        }
        val answer = if (events.isEmpty()) {
            "Khong co event nao $rangeText."
        } else {
            buildString {
                appendLine("Co ${events.size} event $rangeText:")
                events.forEach { event ->
                    val tagText = event.tagName?.takeIf(String::isNotBlank)?.let { " [$it]" }.orEmpty()
                    appendLine("- #${event.id} ${event.startTime.toLocalDate()} ${event.startTime.toLocalTime()}-${event.endTime.toLocalTime()}: ${event.title}$tagText")
                }
            }.trimEnd()
        }
        return AgentChatResponse(
            answer = answer,
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.LIST_EVENTS_TOOL_ID,
                    toolName = ToolRegistryService.LIST_EVENTS_TOOL_NAME,
                    category = "EVENT_READ",
                    params = params + (tagId?.let { mapOf("resolvedTagId" to it.toString()) } ?: emptyMap()),
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun createEventFromAgent(userId: Int, params: Map<String, String>, userMessage: String): AgentChatResponse {
        val rawTitle = params["title"]?.takeIf(String::isNotBlank)
            ?: throw ApiException(400, "title is required")
        val title = cleanEventTitle(rawTitle)
        val startTime = params["startTime"]?.let(::parseLocalDateTime)
            ?: throw ApiException(400, "startTime is required")
        val endTime = params["endTime"]?.let(::parseLocalDateTime)
            ?: throw ApiException(400, "endTime is required")
        val tagId = resolveEventTagId(userId, params, userMessage)

        val result = eventService.createEvent(
            CreateEventRequest(
                title = title,
                description = params["description"]?.takeIf(String::isNotBlank),
                startTime = startTime,
                endTime = endTime,
                tagId = tagId,
                userId = userId
            ),
            userId
        )
        if (result.code !in 200..299) {
            throw ApiException(result.code, result.message)
        }

        val eventId = result.body?.id?.let { " #$it" }.orEmpty()
        return AgentChatResponse(
            answer = "Đã thêm event$eventId: $title từ $startTime đến $endTime.",
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.CREATE_EVENT_TOOL_ID,
                    toolName = ToolRegistryService.CREATE_EVENT_TOOL_NAME,
                    category = "EVENT_WRITE",
                    params = params + mapOf("title" to title) + (tagId?.let { mapOf("resolvedTagId" to it.toString()) } ?: emptyMap()),
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun parseLocalDateTime(value: String): LocalDateTime =
        runCatching { LocalDateTime.parse(value.trim()) }
            .recoverCatching { LocalDateTime.parse(value.trim().replace(' ', 'T')) }
            .getOrElse { throw ApiException(400, "invalid datetime: $value") }

    private fun deleteEventFromAgent(userId: Int, params: Map<String, String>): AgentChatResponse {
        val eventId = params["eventId"]?.toIntOrNull()
            ?: throw ApiException(400, "eventId is required")
        val result = eventService.deleteEvent(eventId, userId)
        if (result.code !in 200..299) {
            throw ApiException(result.code, result.message)
        }
        return AgentChatResponse(
            answer = "Đã xóa event #$eventId.",
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.DELETE_EVENT_TOOL_ID,
                    toolName = ToolRegistryService.DELETE_EVENT_TOOL_NAME,
                    category = "EVENT_DELETE",
                    params = params,
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun importPortalScheduleFromAgent(
        userId: Int,
        sessionToken: String,
        params: Map<String, String>,
        userMessage: String
    ): AgentChatResponse {
        val startDate = params["startDate"]?.let(LocalDate::parse)
            ?: throw ApiException(400, "startDate is required")
        val endDate = params["endDate"]?.let(LocalDate::parse)
            ?: throw ApiException(400, "endDate is required")
        if (endDate.isBefore(startDate)) throw ApiException(400, "endDate must be on or after startDate")

        val scheduleTool = selectPortalScheduleTool(userId)
        val credentials = credentialResolver.resolve(
            userId = userId,
            requiredHeaders = scheduleTool.requiredCredentialHeaders,
            optionalHeaders = scheduleTool.optionalCredentialHeaders
        )
        if (credentials.missingRequiredHeaders.isNotEmpty()) {
            return AgentChatResponse(
                answer = "Bạn cần đăng nhập portal trước để tôi import lịch.",
                needsClarification = true,
                missing = credentials.missingRequiredHeaders
            )
        }

        val seedParams = scheduleSeedParams(scheduleTool, startDate, endDate)
        val runParams = scheduleRunParams(seedParams, startDate to endDate)
        val runResults = runParams.map { paramsForRun ->
            runTool(
                userId = userId,
                sessionToken = sessionToken,
                toolId = scheduleTool.id,
                category = scheduleTool.category,
                method = scheduleTool.method,
                params = paramsForRun,
                credentials = credentials.headers,
                headers = emptyMap(),
                body = null
            )
        }
        val normalized = combineNormalized(runResults.map { normalizers.normalize(scheduleTool, it) })
        val tagId = resolveEventTagId(userId, params + ("tagName" to params.getOrDefault("tagName", "Study")), userMessage)
        val eventInputs = normalized.items
            .mapNotNull { item -> scheduleItemToEventInput(item, startDate, endDate, tagId, userId) }
            .distinctBy { "${it.title}|${it.startTime}|${it.endTime}" }
        val created = eventInputs
            .mapNotNull { input ->
                val result = eventService.createEvent(input, userId)
                if (result.code in 200..299) result.body?.id else null
            }
        val skippedWithoutTime = normalized.items.size - eventInputs.size
        val answer = if (created.isEmpty() && skippedWithoutTime > 0) {
            "Khong import event vi du lieu lich portal hien tai khong co gio hoc chi tiet. Hay dung schedule tool tra ve tuGio/denGio hoac endpoint chi tiet thay vi lich thang."
        } else {
            "Da import ${created.size} event tu lich portal vao Smart Calendar."
        }

        return AgentChatResponse(
            answer = answer,
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.IMPORT_PORTAL_SCHEDULE_TOOL_ID,
                    toolName = ToolRegistryService.IMPORT_PORTAL_SCHEDULE_TOOL_NAME,
                    category = "SCHEDULE_IMPORT",
                    params = params + mapOf(
                        "scheduleToolId" to scheduleTool.id.toString(),
                        "createdEventIds" to created.joinToString(","),
                        "skippedWithoutTime" to skippedWithoutTime.toString()
                    ) + (tagId?.let { mapOf("resolvedTagId" to it.toString()) } ?: emptyMap()),
                    upstreamStatus = runResults.firstOrNull()?.status
                )
            )
        )
    }

    private fun selectPortalScheduleTool(userId: Int): AgentToolDescriptor =
        toolRegistryService.getToolsForUser(userId)
            .filter { it.category.equals("SCHEDULE", ignoreCase = true) }
            .sortedWith(
                compareByDescending<AgentToolDescriptor> { "portal.ut.edu.vn" in it.urlTemplate }
                    .thenByDescending { scheduleDetailPriority(it.urlTemplate) }
                    .thenByDescending { it.scope == "CUSTOM" }
                    .thenByDescending { it.id }
            )
            .firstOrNull()
            ?: throw ApiException(404, "portal schedule tool not found")

    private fun scheduleDetailPriority(urlTemplate: String): Int {
        val lower = urlTemplate.lowercase()
        return when {
            "/lichhoc/ngay" in lower -> 3
            "/lichhoc/tuan" in lower -> 2
            "/lichhoc/thang" in lower -> 0
            else -> 1
        }
    }

    private fun scheduleSeedParams(tool: AgentToolDescriptor, startDate: LocalDate, endDate: LocalDate): Map<String, String> {
        val startDateParamName = tool.requiredParams.firstOrNull { it.equals("startDate", ignoreCase = true) }
        val endDateParamName = tool.requiredParams.firstOrNull { it.equals("endDate", ignoreCase = true) }
        if (startDateParamName != null && endDateParamName != null) {
            return mapOf(startDateParamName to startDate.toString(), endDateParamName to endDate.toString())
        }
        val dateParamName = tool.requiredParams.firstOrNull {
            it.contains("date", ignoreCase = true) || it.contains("ngay", ignoreCase = true)
        } ?: "date"
        return mapOf(dateParamName to startDate.toString())
    }

    private fun scheduleItemToEventInput(
        item: Map<String, Any?>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        tagId: Int?,
        userId: Int
    ): CreateEventRequest? {
        val title = item["title"]?.toString()?.takeIf(String::isNotBlank) ?: return null
        val date = item["date"]?.toString()?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val startText = item["start"]?.toString()
        val endText = item["end"]?.toString()
        if (!hasUsableScheduleTime(startText) || !hasUsableScheduleTime(endText)) return null
        val startTime = scheduleDateTime(date, startText, endOfDay = false) ?: return null
        val endTime = scheduleDateTime(date, endText, endOfDay = true)
            ?: startTime.plusHours(1)
        if (startTime.toLocalDate().isBefore(rangeStart) || startTime.toLocalDate().isAfter(rangeEnd)) return null
        return CreateEventRequest(
            title = title,
            description = item["location"]?.toString()?.takeIf(String::isNotBlank),
            startTime = startTime,
            endTime = if (endTime.isAfter(startTime)) endTime else startTime.plusHours(1),
            tagId = tagId,
            userId = userId
        )
    }

    private fun hasUsableScheduleTime(value: String?): Boolean {
        val text = value?.trim().orEmpty()
        if (text.isBlank()) return false
        if (Regex("""\d{1,2}:\d{2}""").containsMatchIn(text)) return true
        return runCatching { LocalDateTime.parse(text) }.isSuccess
    }

    private fun scheduleDateTime(date: LocalDate?, value: String?, endOfDay: Boolean): LocalDateTime? {
        val text = value?.trim().orEmpty()
        if (text.isNotBlank()) {
            runCatching { return LocalDateTime.parse(text) }
            runCatching { return LocalDate.parse(text.take(10)).atTime(if (endOfDay) LocalTime.of(23, 59) else LocalTime.MIDNIGHT) }
            if (date != null) {
                Regex("""(\d{1,2}):(\d{2})""").find(text)?.let { match ->
                    return date.atTime(match.groupValues[1].toInt(), match.groupValues[2].toInt())
                }
            }
        }
        return date?.atTime(if (endOfDay) LocalTime.of(23, 59) else LocalTime.MIDNIGHT)
    }

    private fun createTagFromAgent(userId: Int, params: Map<String, String>): AgentChatResponse {
        val name = params["name"]?.takeIf(String::isNotBlank)
            ?: throw ApiException(400, "name is required")
        val result = tagService.createTag(
            CreateTagRequest(
                name = cleanTagName(name),
                color = params["color"]?.takeIf(String::isNotBlank),
                userId = userId
            ),
            userId
        )
        if (result.code !in 200..299) throw ApiException(result.code, result.message)
        val tag = result.body
        return AgentChatResponse(
            answer = "Đã tạo tag #${tag?.id}: ${tag?.name ?: cleanTagName(name)}.",
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.CREATE_TAG_TOOL_ID,
                    toolName = ToolRegistryService.CREATE_TAG_TOOL_NAME,
                    category = "TAG_WRITE",
                    params = params + (tag?.id?.let { mapOf("createdTagId" to it.toString(), "name" to tag.name) } ?: emptyMap()),
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun updateTagFromAgent(userId: Int, params: Map<String, String>, userMessage: String): AgentChatResponse {
        val tagId = params["tagId"]?.toIntOrNull()
            ?: throw ApiException(400, "tagId is required")
        val existing = tagRepository.findById(tagId).orElse(null)
            ?: throw ApiException(404, "tag not found")
        if (existing.user?.id != userId) throw ApiException(403, "tag does not belong to the active session")
        val nextName = params["name"]?.takeIf(String::isNotBlank)
            ?: inlineParam("name", userMessage)
            ?: inferRenameTagName(userMessage)
        val nextColor = params["color"]?.takeIf(String::isNotBlank)
            ?: inlineParam("color", userMessage)
            ?: extractHexColor(userMessage)
        if (nextName.isNullOrBlank() && nextColor.isNullOrBlank()) {
            throw ApiException(400, "name or color is required")
        }
        val result = tagService.updateTag(
            tagId,
            UpdateTagRequest(
                name = nextName?.let(::cleanTagName) ?: existing.name,
                color = nextColor ?: existing.color,
                userId = userId
            ),
            userId
        )
        if (result.code !in 200..299) throw ApiException(result.code, result.message)
        val tag = result.body
        return AgentChatResponse(
            answer = "Đã cập nhật tag #$tagId: ${tag?.name ?: existing.name}.",
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.UPDATE_TAG_TOOL_ID,
                    toolName = ToolRegistryService.UPDATE_TAG_TOOL_NAME,
                    category = "TAG_UPDATE",
                    params = params +
                        (nextName?.let { mapOf("name" to cleanTagName(it)) } ?: emptyMap()) +
                        (nextColor?.let { mapOf("color" to it) } ?: emptyMap()),
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun deleteTagFromAgent(userId: Int, params: Map<String, String>): AgentChatResponse {
        val tagId = params["tagId"]?.toIntOrNull()
            ?: throw ApiException(400, "tagId is required")
        val result = tagService.deleteTag(tagId, userId)
        if (result.code !in 200..299) throw ApiException(result.code, result.message)
        return AgentChatResponse(
            answer = "Đã xóa tag #$tagId.",
            toolCalls = listOf(
                AgentToolCallView(
                    toolId = ToolRegistryService.DELETE_TAG_TOOL_ID,
                    toolName = ToolRegistryService.DELETE_TAG_TOOL_NAME,
                    category = "TAG_DELETE",
                    params = params,
                    upstreamStatus = result.code
                )
            )
        )
    }

    private fun cleanTagName(value: String): String =
        value
            .replace(Regex("""(?i)\b(color|màu|mau|tagId|id)\s*[=:]?\s*.+$"""), "")
            .trim()
            .ifBlank { "Untitled tag" }

    private fun resolveEventTagId(userId: Int, params: Map<String, String>, userMessage: String): Int? {
        val searchText = listOfNotNull(userMessage, params["title"], params["description"]).joinToString(" ")
        inlineParam("tagId", searchText)?.toIntOrNull()?.let { return validateTagId(userId, it) }
        params["tagId"]?.toIntOrNull()?.let { tagId ->
            return validateTagId(userId, tagId)
        }
        val tagName = params["tagName"]?.takeIf(String::isNotBlank)
            ?: inlineParam("tagName", searchText)
            ?: inferTagName(searchText)
            ?: return null
        val tags = tagRepository.searchTags(null, userId)
        return tags.firstOrNull { it.name.equals(tagName, ignoreCase = true) }?.id
            ?: tags.firstOrNull {
                it.name.contains(tagName, ignoreCase = true) ||
                    tagName.contains(it.name, ignoreCase = true)
            }?.id
    }

    private fun resolveReadTagId(userId: Int, params: Map<String, String>): Int? {
        params["tagId"]?.toIntOrNull()?.let { return validateTagId(userId, it) }
        val tagName = params["tagName"]?.takeIf(String::isNotBlank) ?: return null
        val tags = tagRepository.searchTags(null, userId)
        return tags.firstOrNull { it.name.equals(tagName, ignoreCase = true) }?.id
            ?: tags.firstOrNull {
                it.name.contains(tagName, ignoreCase = true) ||
                    tagName.contains(it.name, ignoreCase = true)
            }?.id
    }

    private fun formatDateRange(startDate: LocalDate, endDate: LocalDate): String =
        if (startDate == endDate) startDate.toString() else "$startDate - $endDate"

    private fun validateTagId(userId: Int, tagId: Int): Int {
            val tag = tagRepository.findById(tagId).orElse(null)
                ?: throw ApiException(404, "tag not found")
            if (tag.user?.id != userId) throw ApiException(403, "tag does not belong to the active session")
            return tagId
    }

    private fun inferTagName(value: String): String? {
        val lower = value.lowercase()
        return when {
            listOf("study", "hoc", "học", "on bai", "ôn bài", "lop", "lớp", "exam", "thi").any(lower::contains) -> "Study"
            listOf("work", "meeting", "hop", "họp", "lam viec", "làm việc", "deadline").any(lower::contains) -> "Work"
            listOf("health", "gym", "doctor", "khám", "kham", "suc khoe", "sức khỏe").any(lower::contains) -> "Health"
            listOf("personal", "ca nhan", "cá nhân", "family", "gia dinh", "gia đình").any(lower::contains) -> "Personal"
            else -> null
        }
    }

    private fun cleanEventTitle(value: String): String =
        value
            .replace(INLINE_EVENT_PARAM, "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .ifBlank { "Untitled event" }

    private fun inlineParam(name: String, value: String?): String? {
        if (value.isNullOrBlank()) return null
        val match = Regex("""(?i)\b${Regex.escape(name)}\s*=\s*(.+?)(?=\s+(title|startTime|endTime|tagId|tagName|description|name|color|eventId)\s*=|$)""")
            .find(value)
            ?: return null
        return match.groupValues[1].trim().takeIf(String::isNotBlank)
    }

    private fun extractHexColor(value: String): String? =
        Regex("""(?i)(#[0-9a-f]{6}|#[0-9a-f]{3})""")
            .find(value)
            ?.groupValues
            ?.get(1)

    private fun inferRenameTagName(value: String): String? {
        val direct = Regex("""(?i)\b(?:to|thành|thanh|sang)\s+(.+?)(?=\s+(color|màu|mau)\b|\s+#[0-9a-f]{3,6}\b|$)""")
            .find(value)
            ?.groupValues
            ?.get(1)
            ?.trim()
        if (!direct.isNullOrBlank()) return direct
        return Regex("""(?i)\b(?:rename|đổi tên|doi ten|update|cập nhật|cap nhat)\s+tag(?:Id)?\s*=?\s*\d*\s*(.+?)(?=\s+(color|màu|mau)\b|\s+#[0-9a-f]{3,6}\b|$)""")
            .find(value)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    companion object {
        private val INLINE_EVENT_PARAM =
            Regex("""(?i)\b(startTime|endTime|tagId|tagName|description)\s*=\s*.+?(?=\s+(title|startTime|endTime|tagId|tagName|description)\s*=|$)""")
    }

    private fun runTool(
        userId: Int,
        sessionToken: String,
        toolId: Int,
        category: String,
        method: String,
        params: Map<String, String>,
        credentials: Map<String, String>,
        headers: Map<String, String>,
        body: com.fasterxml.jackson.databind.JsonNode?
    ): KnownToolRunResult {
        var result: KnownToolRunResult? = null
        val duration = measureTimeMillis {
            result = runnerClient.runById(
                sessionToken = sessionToken,
                toolId = toolId,
                params = params,
                credentials = credentials,
                headers = headers,
                body = body
            )
        }
        log.info(
            "agent_tool_run userId={} toolId={} category={} method={} params={} upstreamStatus={} durationMs={} resultBody={}",
            userId,
            toolId,
            category,
            method,
            params.keys,
            result?.status,
            duration,
            result?.body?.size() ?: result?.rawBody?.length ?: 0
        )
        return result ?: throw ApiException(500, "known tool runner returned no result")
    }

    private fun scheduleRunParams(
        params: Map<String, String>,
        range: Pair<LocalDate, LocalDate>?
    ): List<Map<String, String>> {
        if (range == null) return listOf(params)

        val startDateParamName = params.keys.firstOrNull { it.equals("startDate", ignoreCase = true) }
        val endDateParamName = params.keys.firstOrNull { it.equals("endDate", ignoreCase = true) }
        if (startDateParamName != null && endDateParamName != null) {
            return listOf(
                params + mapOf(
                    startDateParamName to range.first.toString(),
                    endDateParamName to range.second.toString()
                )
            )
        }

        val dateParamName = params.keys.firstOrNull { it.contains("date", ignoreCase = true) || it.contains("ngay", ignoreCase = true) }
            ?: "date"
        val runs = mutableListOf<Map<String, String>>()
        var cursor = range.first
        while (!cursor.isAfter(range.second)) {
            runs += params + (dateParamName to cursor.toString())
            cursor = cursor.plusDays(7)
        }
        return runs.distinctBy { it[dateParamName] }
    }

    private fun combineNormalized(results: List<NormalizedToolResult>): NormalizedToolResult {
        if (results.isEmpty()) return NormalizedToolResult("", "", emptyList(), true)
        val items = results.flatMap { it.items }.distinctBy { it.entries.joinToString("|") { entry -> "${entry.key}=${entry.value}" } }
        return results.first().copy(
            summary = "${items.size} mục.",
            items = items,
            empty = items.isEmpty()
        )
    }
}
