package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CandidateTrace
import com.example.smartcalendar.portaldiscovery.CapturedExchange
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.fasterxml.jackson.databind.ObjectMapper

data class Endpoint(val id: String, val method: String, val url: String, val sample: CapturedExchange) {
    fun snapshot(json: ObjectMapper): String = json.writeValueAsString(
        mapOf("method" to method, "url" to url, "contentType" to sample.responseContentType, "responseSample" to sample.responseBody?.take(20_000)),
    )

    fun trace(blocked: Boolean, heuristic: Double, category: EndpointCategory = EndpointCategory.OTHER, percival: Double? = null, merlin: String? = null, decision: String) =
        CandidateTrace(id, method, url, category, blocked, heuristic, percival, merlin, decision)
}

data class SafetyResult(val blocked: Boolean, val allowed: Boolean)
data class SignalResult(val candidate: Boolean, val confidence: Double, val suggestedCategory: EndpointCategory)
data class ReplayResult(
    val success: Boolean,
    val body: String? = null,
    val sessionExpired: Boolean = false,
    val reason: String? = null,
    val requiredHeaderNames: Set<String> = emptySet(),
)
