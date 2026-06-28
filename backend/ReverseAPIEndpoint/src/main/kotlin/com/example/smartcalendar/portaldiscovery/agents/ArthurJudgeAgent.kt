package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.core.SafetyResult

class ArthurJudgeAgent {
    fun decideCandidate(safety: SafetyResult, candidate: PercivalCandidate, merlinVerdict: String): String = when {
        safety.blocked || !safety.allowed -> "REJECT"
        candidate.category == EndpointCategory.DANGEROUS_WRITE || candidate.category == EndpointCategory.OTHER -> "REJECT"
        merlinVerdict == "UNCERTAIN" -> "MANUAL_REVIEW"
        merlinVerdict != "PASS" || !candidate.isCandidate || candidate.confidence < 0.75 -> "REJECT"
        else -> "APPROVE"
    }

    fun approveMapping(mapping: CalendarMapping, eventCount: Int, merlinVerdict: String): Boolean =
        merlinVerdict == "PASS" && mapping.confidence >= 0.75 && eventCount > 0
}
