package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.fasterxml.jackson.databind.ObjectMapper

data class EndpointClassification(val category: EndpointCategory, val confidence: Double, val rawJson: String)

class PalamedesClassifierAgent(private val router: AgentLlmRouter, private val json: ObjectMapper) {
    suspend fun classify(endpoint: Endpoint): EndpointClassification {
        val raw = router.palamedes(
            """{"category":"SCHEDULE|REGISTERED_COURSES|AVAILABLE_COURSES|RETAKE_COURSES|NOTIFICATION|SEMESTER|DANGEROUS_WRITE|OTHER","confidence":0.0,"reason":"string"}""",
            endpoint.snapshot(json),
        )
        val node = json.readTree(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
        val category = EndpointCategory.entries.firstOrNull { it.name == node.path("category").asText().uppercase() }
            ?: EndpointCategory.OTHER
        return EndpointClassification(category, node.path("confidence").asDouble(0.0), raw)
    }
}
