package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.ToolExample
import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.repository.ApiKnowledgeJpaRepository
import com.example.smartcalendar.repository.EventRepository
import com.example.smartcalendar.repository.TagRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ToolRegistryService(
    private val repository: ApiKnowledgeJpaRepository,
    private val eventRepository: EventRepository,
    private val tagRepository: TagRepository,
    private val json: ObjectMapper
) {
    fun getToolsForUser(userId: Int): List<AgentToolDescriptor> =
        internalTools(userId) +
            (repository.findAllByUserIsNullOrderByUpdatedAtDesc() + repository.findAllByUserIdOrderByUpdatedAtDesc(userId))
                .sortedByDescending(ApiKnowledgeEntity::updatedAt)
                .mapNotNull(::toDescriptor)

    private fun toDescriptor(entity: ApiKnowledgeEntity): AgentToolDescriptor? {
        val knowledge = runCatching { json.readValue(entity.knowledgeJson, ApiKnowledge::class.java) }.getOrNull()
            ?: return null
        val url = knowledge.urlTemplate.ifBlank { entity.portalUrl }
        val requiredParams = if (knowledge.requiredParams.isNotEmpty()) {
            knowledge.requiredParams.map { it.name }
        } else {
            URL_PARAM.findAll(url).map { it.groupValues[1] }.toList()
        }
        val safetyLevel = knowledge.safetyLevel.name
        return AgentToolDescriptor(
            id = entity.id,
            toolName = entity.toolName,
            scope = if (entity.user == null) "GLOBAL" else "CUSTOM",
            category = entity.category,
            method = entity.method.uppercase(),
            urlTemplate = url,
            description = knowledge.purpose,
            requiredParams = requiredParams,
            optionalParams = knowledge.optionalParams.map { it.name },
            requiredCredentialHeaders = knowledge.headersPolicy.requiredCredentialHeaders,
            optionalCredentialHeaders = knowledge.headersPolicy.requiredStaticHeaders,
            bodySchema = knowledge.requestBodySchema?.let { runCatching { json.readTree(it) }.getOrNull() },
            safetyLevel = safetyLevel,
            readOnly = safetyLevel == "READ_ONLY",
            examples = examples(entity.category, requiredParams)
        )
    }

    private fun examples(category: String, params: List<String>): List<ToolExample> = when (category.uppercase()) {
        "EVENT_WRITE" -> listOf(
            ToolExample(
                "add team meeting from 2026-07-01T08:00:00 to 2026-07-01T09:00:00",
                mapOf("title" to "team meeting", "startTime" to "2026-07-01T08:00:00", "endTime" to "2026-07-01T09:00:00")
            )
        )
        "EVENT_DELETE" -> listOf(
            ToolExample(
                "delete event 17",
                mapOf("eventId" to "17")
            )
        )
        "SCHEDULE_IMPORT" -> listOf(
            ToolExample(
                "import portal schedule from 2026-06-20 to 2026-06-30 into events",
                mapOf("startDate" to "2026-06-20", "endDate" to "2026-06-30", "tagName" to "Study")
            )
        )
        "TAG_WRITE" -> listOf(
            ToolExample(
                "create tag Fitness color #9C27B0",
                mapOf("name" to "Fitness", "color" to "#9C27B0")
            )
        )
        "TAG_UPDATE" -> listOf(
            ToolExample(
                "rename tag 4 to Fitness",
                mapOf("tagId" to "4", "name" to "Fitness")
            )
        )
        "TAG_DELETE" -> listOf(
            ToolExample(
                "delete tag 4",
                mapOf("tagId" to "4")
            )
        )
        "SCHEDULE" -> listOf(ToolExample("hôm nay tôi học gì", mapOf("date" to "today")))
        "SEMESTER" -> listOf(ToolExample("có học kỳ nào"))
        "NOTIFICATION" -> listOf(ToolExample("lấy danh mục thông báo"))
        "REGISTERED_COURSES" -> listOf(ToolExample("tôi đã đăng ký môn nào đợt 76", mapOf("idDot" to "76")))
        "RETAKE_COURSES" -> listOf(ToolExample("có môn cải thiện nào đợt 76 không", mapOf("idDot" to "76")))
        "LOGIN" -> listOf(ToolExample("đăng nhập portal"))
        else -> if (params.isEmpty()) emptyList() else listOf(ToolExample("chạy ${category.lowercase()}"))
    }

    private fun internalTools(userId: Int): List<AgentToolDescriptor> {
        val tagSummary = tagRepository.searchTags(null, userId)
            .joinToString("; ") { tag -> "${tag.id}:${tag.name}" }
            .ifBlank { "none" }
        val eventSummary = eventRepository.searchEvents(
            keyword = null,
            tagId = null,
            userId = userId,
            from = LocalDateTime.now().minusDays(30),
            to = LocalDateTime.now().plusDays(365)
        )
            .take(30)
            .joinToString("; ") { event ->
                "${event.id}:${event.title} ${event.startTime}-${event.endTime}${event.tag?.name?.let { " tag=$it" }.orEmpty()}"
            }
            .ifBlank { "none" }
        return listOf(
            AgentToolDescriptor(
                id = CREATE_EVENT_TOOL_ID,
                toolName = CREATE_EVENT_TOOL_NAME,
                scope = "INTERNAL",
                category = "EVENT_WRITE",
                method = "INTERNAL",
                urlTemplate = "internal://events/create",
                description = "Create a Smart Calendar event for the active user after confirmation. Available tags for this user: $tagSummary. Choose the best tag by returning tagId or tagName when a tag clearly matches; otherwise omit tag fields. Create a clean title without datetime or parameter text.",
                requiredParams = listOf("title", "startTime", "endTime"),
                optionalParams = listOf("description", "tagId", "tagName"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("EVENT_WRITE", emptyList())
            ),
            AgentToolDescriptor(
                id = DELETE_EVENT_TOOL_ID,
                toolName = DELETE_EVENT_TOOL_NAME,
                scope = "INTERNAL",
                category = "EVENT_DELETE",
                method = "INTERNAL",
                urlTemplate = "internal://events/delete",
                description = "Delete one Smart Calendar event for the active user after confirmation. Available events for this user: $eventSummary. Return eventId only when exactly one event clearly matches the user request. If ambiguous, ask for clarification.",
                requiredParams = listOf("eventId"),
                optionalParams = listOf("reason"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("EVENT_DELETE", emptyList())
            ),
            AgentToolDescriptor(
                id = IMPORT_PORTAL_SCHEDULE_TOOL_ID,
                toolName = IMPORT_PORTAL_SCHEDULE_TOOL_NAME,
                scope = "INTERNAL",
                category = "SCHEDULE_IMPORT",
                method = "INTERNAL",
                urlTemplate = "internal://events/import-portal-schedule",
                description = "Fetch the user's portal schedule through the available SCHEDULE portal tool, then create Smart Calendar events after confirmation. Use this when the user asks to get/import/sync portal schedule into events. Available tags for this user: $tagSummary. Prefer Study tag unless the user clearly asks for another tag.",
                requiredParams = listOf("startDate", "endDate"),
                optionalParams = listOf("tagId", "tagName"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("SCHEDULE_IMPORT", emptyList())
            ),
            AgentToolDescriptor(
                id = CREATE_TAG_TOOL_ID,
                toolName = CREATE_TAG_TOOL_NAME,
                scope = "INTERNAL",
                category = "TAG_WRITE",
                method = "INTERNAL",
                urlTemplate = "internal://tags/create",
                description = "Create a Smart Calendar tag for the active user after confirmation. Existing tags for this user: $tagSummary. Return a short unique name and optional CSS hex color.",
                requiredParams = listOf("name"),
                optionalParams = listOf("color"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("TAG_WRITE", emptyList())
            ),
            AgentToolDescriptor(
                id = UPDATE_TAG_TOOL_ID,
                toolName = UPDATE_TAG_TOOL_NAME,
                scope = "INTERNAL",
                category = "TAG_UPDATE",
                method = "INTERNAL",
                urlTemplate = "internal://tags/update",
                description = "Update one Smart Calendar tag for the active user after confirmation. Existing tags for this user: $tagSummary. Return tagId only when exactly one tag clearly matches. Return new name and/or color.",
                requiredParams = listOf("tagId"),
                optionalParams = listOf("name", "color"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("TAG_UPDATE", emptyList())
            ),
            AgentToolDescriptor(
                id = DELETE_TAG_TOOL_ID,
                toolName = DELETE_TAG_TOOL_NAME,
                scope = "INTERNAL",
                category = "TAG_DELETE",
                method = "INTERNAL",
                urlTemplate = "internal://tags/delete",
                description = "Delete one Smart Calendar tag for the active user after confirmation. Existing tags for this user: $tagSummary. Return tagId only when exactly one tag clearly matches. If ambiguous, ask for clarification.",
                requiredParams = listOf("tagId"),
                optionalParams = listOf("reason"),
                requiredCredentialHeaders = emptyList(),
                optionalCredentialHeaders = emptyList(),
                bodySchema = null,
                safetyLevel = "USER_CONFIRM_REQUIRED",
                readOnly = false,
                examples = examples("TAG_DELETE", emptyList())
            )
        )
    }

    companion object {
        private val URL_PARAM = Regex("""\{([^}]+)}""")
        const val CREATE_EVENT_TOOL_ID = -1001
        const val CREATE_EVENT_TOOL_NAME = "create_event"
        const val DELETE_EVENT_TOOL_ID = -1002
        const val DELETE_EVENT_TOOL_NAME = "delete_event"
        const val IMPORT_PORTAL_SCHEDULE_TOOL_ID = -1003
        const val IMPORT_PORTAL_SCHEDULE_TOOL_NAME = "import_portal_schedule"
        const val CREATE_TAG_TOOL_ID = -2001
        const val CREATE_TAG_TOOL_NAME = "create_tag"
        const val UPDATE_TAG_TOOL_ID = -2002
        const val UPDATE_TAG_TOOL_NAME = "update_tag"
        const val DELETE_TAG_TOOL_ID = -2003
        const val DELETE_TAG_TOOL_NAME = "delete_tag"
    }
}

