package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class NinerouterPlanResult(
    val action: String?,
    val toolId: Int?,
    val toolName: String?,
    val category: String?,
    val params: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: Map<String, Any?>? = null,
    val answerHint: String?,
    val reason: String?
)

@Service
class NinerouterAgentPlannerClient(
    private val json: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(NinerouterAgentPlannerClient::class.java)
    private val client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    fun plan(
        userMessage: String,
        tools: List<AgentToolDescriptor>,
        currentDate: String,
        timezone: String
    ): NinerouterPlanResult? {
        if (!enabled()) return null

        val body = json.writeValueAsString(
            mapOf(
                "model" to model(),
                "stream" to false,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt()),
                    mapOf("role" to "user", "content" to userPrompt(userMessage, tools, currentDate, timezone))
                )
            )
        )
        val headers = mutableMapOf("Content-Type" to "application/json")
        apiKey().takeIf { it.isNotBlank() }?.let { headers["Authorization"] = "Bearer $it" }

        return runCatching {
            val request = HttpRequest.newBuilder(URI("${apiBase()}/chat/completions"))
                .headers(*headers.flatMap { listOf(it.key, it.value) }.toTypedArray())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                log.warn("ninerouter_agent_plan_failed status={} body={}", response.statusCode(), response.body().take(500))
                return null
            }
            val content = json.readTree(response.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText()
            json.readValue(extractJsonObject(content), NinerouterPlanResult::class.java)
        }.getOrElse { exception ->
            log.warn("ninerouter_agent_plan_failed error={}", exception.message)
            null
        }
    }

    private fun systemPrompt() = """
        You are the Smart Calendar tool planner.
        Return exactly one JSON object, no markdown.
        Schema:
        {
          "action": "RUN_TOOL" | "NEED_CLARIFICATION" | "NEED_CONFIRMATION" | "REFUSED",
          "toolId": number|null,
          "toolName": string|null,
          "category": string|null,
          "params": {"paramName":"value"},
          "headers": {"headerName":"value"},
          "body": object|null,
          "answerHint": string|null,
          "reason": string|null
        }
        Only select a tool from the provided tool list. Never invent URLs or tool names.
        For READ_ONLY tools, use RUN_TOOL when required params can be resolved.
        For LOGIN or USER_CONFIRM_REQUIRED tools, use NEED_CONFIRMATION.
        For the internal list_events tool, return:
        - toolName=list_events
        - category=EVENT_READ
        - optional params.startDate and params.endDate as ISO dates, for example 2026-07-01.
        - omit startDate and endDate when the user asks for all events.
        - optional params.tagId or params.tagName when the user filters by tag.
        - optional params.keyword when the user searches by event title.
        Use this tool when the user asks what Smart Calendar events they have, asks for existing events, or explicitly says not to use portal.
        Do not use SCHEDULE portal tools when the user asks for Smart Calendar events or says not to use portal.
        Default calendar/event questions must use Smart Calendar events via list_events. Only use SCHEDULE portal tools or import_portal_schedule if the user explicitly includes the word "portal".
        For the internal create_event tool, return:
        - toolName=create_event
        - category=EVENT_WRITE
        - params.title as a short event title
        - params.startTime and params.endTime as ISO-8601 local datetimes, for example 2026-07-01T08:00:00.
        - params.tagId or params.tagName when the message clearly matches one of the available tags in the tool description.
        Do not include datetime, parameter names, or tag metadata inside params.title.
        If the user asks to add/create/schedule an event but does not provide enough date or time details, use NEED_CLARIFICATION.
        For the internal delete_event tool, return:
        - toolName=delete_event
        - category=EVENT_DELETE
        - params.eventId from the available events in the tool description.
        If the user asks to delete/remove/cancel an event but the matching event is ambiguous, use NEED_CLARIFICATION and ask which event id to delete.
        For the internal import_portal_schedule tool, return:
        - toolName=import_portal_schedule
        - category=SCHEDULE_IMPORT
        - params.startDate and params.endDate as ISO dates, for example 2026-06-20.
        - optional params.tagId or params.tagName when a tag clearly matches; prefer Study for school schedules.
        Use this tool, not a read-only SCHEDULE tool, when the user asks to fetch/import/sync/add portal schedule into Smart Calendar events and explicitly includes the word "portal".
        For internal tag tools:
        - create_tag uses category=TAG_WRITE and params.name, optional params.color as a CSS hex color.
        - update_tag uses category=TAG_UPDATE and params.tagId from the available tags, plus params.name and/or params.color.
        - delete_tag uses category=TAG_DELETE and params.tagId from the available tags.
        If the user asks to update/delete a tag and the matching tag is ambiguous, use NEED_CLARIFICATION.
        Resolve relative dates using currentDate and timezone.
        If required params are missing, use NEED_CLARIFICATION.
    """.trimIndent()

    private fun userPrompt(
        userMessage: String,
        tools: List<AgentToolDescriptor>,
        currentDate: String,
        timezone: String
    ): String {
        val compactTools = tools.map {
            mapOf(
                "id" to it.id,
                "toolName" to it.toolName,
                "category" to it.category,
                "method" to it.method,
                "urlTemplate" to it.urlTemplate,
                "description" to it.description,
                "requiredParams" to it.requiredParams,
                "optionalParams" to it.optionalParams,
                "bodySchema" to it.bodySchema,
                "safetyLevel" to it.safetyLevel,
                "readOnly" to it.readOnly,
                "examples" to it.examples
            )
        }
        return json.writeValueAsString(
            mapOf(
                "currentDate" to currentDate,
                "timezone" to timezone,
                "userMessage" to userMessage,
                "tools" to compactTools
            )
        )
    }

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        require(start >= 0 && end > start) { "LLM response did not contain JSON object" }
        return trimmed.substring(start, end + 1)
    }

    private fun apiBase(): String =
        property("NINEROUTER_URL", "http://localhost:20128").trimEnd('/').let {
            if (it.endsWith("/v1")) it else "$it/v1"
        }

    private fun apiKey(): String = property("NINEROUTER_KEY", "")

    private fun model(): String = property("NINEROUTER_AGENT_CHAT_MODEL", "openrouter-combo")

    private fun enabled(): Boolean = property("NINEROUTER_AGENT_CHAT_ENABLED", "true").equals("true", ignoreCase = true)

    private fun property(name: String, default: String): String =
        System.getProperty(name)?.takeIf { it.isNotBlank() }
            ?: System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: default
}
