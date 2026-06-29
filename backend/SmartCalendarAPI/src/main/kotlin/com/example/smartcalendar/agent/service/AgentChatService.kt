package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentAction
import com.example.smartcalendar.agent.dto.AgentChatResponse
import com.example.smartcalendar.agent.dto.AgentToolCallView
import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.agent.dto.NormalizedToolResult
import com.example.smartcalendar.agent.normalizer.ToolResponseNormalizerRegistry
import com.example.smartcalendar.agent.security.SafetyGate
import com.example.smartcalendar.agent.util.DateParamResolver
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
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
    private val loginCredentialPersistService: LoginCredentialPersistService
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
        val tools = selectedToolId?.let { id ->
            val selected = availableTools.firstOrNull { it.id == id }
                ?: throw ApiException(404, "selected tool not found for active user")
            listOf(selected)
        } ?: availableTools
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
