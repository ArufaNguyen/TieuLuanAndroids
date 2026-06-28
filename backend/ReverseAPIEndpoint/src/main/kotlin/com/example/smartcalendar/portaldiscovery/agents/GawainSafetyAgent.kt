package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.SafetyResult

class GawainSafetyAgent {
    fun review(endpoint: Endpoint): SafetyResult {
        val url = endpoint.url.lowercase()
        val blocked = endpoint.method in BLOCKED_METHODS ||
            ALWAYS_WRITE_ACTION_WORDS.any(url::contains) ||
            (endpoint.method != "GET" && WRITE_ACTION_WORDS.any(url::contains))
        val allowed = endpoint.method == "GET" || endpoint.method == "POST" && CONDITIONAL_POST.any(url::contains)
        return SafetyResult(blocked, allowed && !blocked)
    }

    companion object {
        val BLOCKED_METHODS = setOf("DELETE", "PUT", "PATCH")
        val ALWAYS_WRITE_ACTION_WORDS = setOf(
            "/delete", "/remove", "/cancel", "/register", "/enroll", "/update", "/save", "/submit",
            "/xoa", "/huy", "/capnhat", "/cap-nhat",
        )
        val WRITE_ACTION_WORDS = setOf(
            "delete", "remove", "drop", "cancel", "register", "enroll", "update", "save", "submit",
            "dangky", "dang-ky", "xoa", "huy", "capnhat", "cap-nhat",
        )
        val CONDITIONAL_POST = setOf("search", "query", "filter")
    }
}
