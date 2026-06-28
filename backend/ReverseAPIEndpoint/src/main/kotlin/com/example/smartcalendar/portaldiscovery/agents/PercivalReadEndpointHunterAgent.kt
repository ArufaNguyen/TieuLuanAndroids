package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.SignalResult
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.fasterxml.jackson.databind.ObjectMapper

data class PercivalCandidate(
    val isCandidate: Boolean,
    val category: EndpointCategory,
    val confidence: Double,
    val rawJson: String,
)

class PercivalReadEndpointHunterAgent(private val router: AgentLlmRouter, private val json: ObjectMapper) {
    suspend fun hunt(endpoint: Endpoint, signal: SignalResult, classification: EndpointClassification): PercivalCandidate {
        val raw = router.percival(
            """{"isReadCandidate":true,"category":"SCHEDULE|REGISTERED_COURSES|AVAILABLE_COURSES|RETAKE_COURSES|NOTIFICATION|SEMESTER|DANGEROUS_WRITE|OTHER","confidence":0.0,"evidence":[],"missingEvidence":[]}""",
            """{"endpoint":${endpoint.snapshot(json)},"heuristic":${json.writeValueAsString(signal)},"classification":${classification.rawJson}}""",
        )
        val node = json.readTree(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
        val llmCategory = EndpointCategory.entries.firstOrNull { it.name == node.path("category").asText().uppercase() }
            ?: classification.category
        val category = if (
            signal.confidence >= 0.55 &&
            signal.suggestedCategory in READ_CATEGORIES
        ) signal.suggestedCategory else llmCategory
        return PercivalCandidate(node.path("isReadCandidate").asBoolean(false), category, node.path("confidence").asDouble(0.0), raw)
    }

    private companion object {
        val READ_CATEGORIES = setOf(
            EndpointCategory.SCHEDULE,
            EndpointCategory.REGISTERED_COURSES,
            EndpointCategory.AVAILABLE_COURSES,
            EndpointCategory.RETAKE_COURSES,
            EndpointCategory.NOTIFICATION,
            EndpointCategory.SEMESTER,
        )
    }
}
