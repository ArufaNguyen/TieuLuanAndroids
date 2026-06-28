package com.example.smartcalendar.agent.dto

import com.fasterxml.jackson.databind.JsonNode

data class AgentChatRequest(
    val message: String,
    val toolId: Int? = null,
    val confirmed: Boolean = false
)

data class AgentChatResponse(
    val answer: String,
    val toolCalls: List<AgentToolCallView> = emptyList(),
    val needsConfirmation: Boolean = false,
    val needsClarification: Boolean = false,
    val pendingConfirmationId: String? = null,
    val missing: List<String> = emptyList()
)

data class AgentToolCallView(
    val toolId: Int,
    val toolName: String,
    val category: String,
    val params: Map<String, String> = emptyMap(),
    val upstreamStatus: Int? = null
)

enum class AgentAction {
    RUN_TOOL,
    NEED_CLARIFICATION,
    NEED_CONFIRMATION,
    REFUSED
}

data class AgentPlan(
    val action: AgentAction,
    val toolCalls: List<AgentToolCall> = emptyList(),
    val answerHint: String? = null,
    val reason: String? = null
)

data class AgentToolCall(
    val toolId: Int,
    val toolName: String,
    val category: String,
    val params: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: JsonNode? = null,
    val confidence: Double = 0.0,
    val reason: String? = null
)

data class AgentToolDescriptor(
    val id: Int,
    val toolName: String,
    val scope: String,
    val category: String,
    val method: String,
    val urlTemplate: String,
    val description: String?,
    val requiredParams: List<String>,
    val optionalParams: List<String>,
    val requiredCredentialHeaders: List<String>,
    val optionalCredentialHeaders: List<String>,
    val bodySchema: JsonNode?,
    val safetyLevel: String,
    val readOnly: Boolean,
    val examples: List<ToolExample> = emptyList()
)

data class ToolExample(
    val userMessage: String,
    val params: Map<String, String> = emptyMap()
)

data class PlanValidation(
    val allowed: Boolean,
    val needsConfirmation: Boolean = false,
    val needsClarification: Boolean = false,
    val refused: Boolean = false,
    val message: String? = null
)

data class ResolvedCredentials(
    val headers: Map<String, String>,
    val missingRequiredHeaders: List<String> = emptyList()
)

data class KnownToolRunResult(
    val toolName: String,
    val resolvedToolName: String,
    val method: String,
    val url: String,
    val status: Int,
    val contentType: String?,
    val body: JsonNode?,
    val rawBody: String?,
    val usedHeaders: List<String>
)

data class NormalizedToolResult(
    val title: String,
    val summary: String,
    val items: List<Map<String, Any?>> = emptyList(),
    val empty: Boolean = false
)
