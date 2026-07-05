package com.example.smartcalendar.portaldiscovery.core

data class HeaderVerificationResult(
    val replay: ReplayResult,
    val requiredCredentialHeaders: Set<String>,
    val requiredStaticHeaders: Set<String>,
)

fun interface HeaderReductionAdvisor {
    suspend fun proposeRemovableHeaders(
        exchange: com.example.smartcalendar.portaldiscovery.CapturedExchange,
        availableHeaderNames: Set<String>,
    ): List<String>
}

class HeaderRequirementVerifier(
    private val replayEngine: ReplayEngine,
    private val advisor: HeaderReductionAdvisor = HeaderReductionAdvisor { _, names -> names.toList() },
) {
    suspend fun verify(exchange: com.example.smartcalendar.portaldiscovery.CapturedExchange): HeaderVerificationResult {
        if (!exchange.method.equals("GET", true)) {
            val replay = replayEngine.replay(exchange)
            return result(replay)
        }
        val available = replayEngine.availableHeaderNames(exchange)
        val fullReplay = replayEngine.replayWithHeaders(exchange, available)
        if (!fullReplay.success) return result(fullReplay)

        minimalCredentialProbes(available).forEach { names ->
            val replay = replayEngine.replayWithHeaders(exchange, names)
            if (replay.success) return result(replay.copy(requiredHeaderNames = names))
        }

        val proposed = runCatching { advisor.proposeRemovableHeaders(exchange, available) }.getOrDefault(emptyList())
        val ordered = (proposed + available.sortedBy(::removalPriority))
            .mapNotNull { proposedName -> available.firstOrNull { it.equals(proposedName, true) } }
            .distinctBy(String::lowercase)
            .take(MAX_PROBES)
        var required = available
        var successfulReplay = fullReplay
        ordered.forEach { removable ->
            val candidate = required.filterNot { it.equals(removable, true) }.toSet()
            val replay = replayEngine.replayWithHeaders(exchange, candidate)
            if (replay.success) {
                required = candidate
                successfulReplay = replay
            }
        }
        return result(successfulReplay.copy(requiredHeaderNames = required))
    }

    private fun result(replay: ReplayResult): HeaderVerificationResult {
        val credentialHeaders = replay.requiredHeaderNames.filter(::isCredentialHeader).map(::canonicalHeaderName).toSet()
        return HeaderVerificationResult(
            replay,
            credentialHeaders,
            replay.requiredHeaderNames.filterNot(::isCredentialHeader).map(::canonicalHeaderName).toSet(),
        )
    }

    private fun minimalCredentialProbes(available: Set<String>): List<Set<String>> {
        val authorization = headerName(available, "Authorization")
        val cookie = headerName(available, "Cookie")
        return buildList {
            add(emptySet())
            authorization?.let { add(setOf(it)) }
            cookie?.let { add(setOf(it)) }
            if (authorization != null && cookie != null) add(setOf(authorization, cookie))
        }.distinct()
    }

    private fun headerName(names: Set<String>, target: String) =
        names.firstOrNull { it.equals(target, true) }

    private fun isCredentialHeader(name: String) =
        name.equals("authorization", true) ||
            name.equals("cookie", true) ||
            name.contains("csrf", true) ||
            name.contains("xsrf", true)

    private fun removalPriority(name: String): Int = when {
        name.equals("authorization", true) -> 100
        name.equals("cookie", true) -> 90
        isCredentialHeader(name) -> 80
        else -> 0
    }

    private fun canonicalHeaderName(name: String): String = when {
        name.equals("authorization", true) -> "Authorization"
        name.equals("cookie", true) -> "Cookie"
        name.equals("accept", true) -> "Accept"
        name.equals("accept-language", true) -> "Accept-Language"
        name.equals("referer", true) -> "Referer"
        name.equals("user-agent", true) -> "User-Agent"
        else -> name.lowercase()
    }

    companion object {
        private const val MAX_PROBES = 16
    }
}
