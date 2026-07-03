package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.fasterxml.jackson.databind.ObjectMapper

class MerlinAdversarialVerifierAgent(private val router: AgentLlmRouter, private val json: ObjectMapper) {
    suspend fun verifyCandidate(endpoint: Endpoint, candidate: PercivalCandidate): String = verdict(
        """{"endpoint":${endpoint.snapshot(json)},"resolvedCategory":"${candidate.category}","percival":${candidate.rawJson}}""",
    )

    suspend fun verifyMapping(mapping: CalendarMapping, eventCount: Int): String = verdict(
        """{"mapping":${json.writeValueAsString(mapping)},"eventCount":$eventCount}""",
    )

    private suspend fun verdict(input: String): String {
        val raw = router.merlin("""{"verdict":"PASS|FAIL|UNCERTAIN","reason":"string"}""", input)
        return json.readTree(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1)).path("verdict").asText("UNCERTAIN").uppercase()
    }
}
