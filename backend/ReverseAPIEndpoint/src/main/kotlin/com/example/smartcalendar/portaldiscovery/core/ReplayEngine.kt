package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CapturedExchange
import com.fasterxml.jackson.databind.ObjectMapper

class ReplayEngine(
    private val json: ObjectMapper,
    private val transport: PortalHttpTransport = DesktopJavaHttpTransport(),
    private val credentials: PortalCredentialProvider = PortalCredentialProvider.NONE,
) {
    fun replay(exchange: CapturedExchange): ReplayResult {
        return replayWithHeaders(exchange, null)
    }

    fun availableHeaderNames(exchange: CapturedExchange): Set<String> =
        mergeHeaders(exchange.requestHeaders, credentials.headersFor(exchange.url))
            .keys
            .filterNot { it.lowercase() in UNSAFE }
            .toSet()

    fun replayWithHeaders(exchange: CapturedExchange, requiredHeaderNames: Set<String>?): ReplayResult {
        val credentialHeaders = credentials.headersFor(exchange.url)
        val headers = mergeHeaders(exchange.requestHeaders, credentialHeaders)
            .filterKeys { it.lowercase() !in UNSAFE }
            .filterKeys { requiredHeaderNames == null || requiredHeaderNames.any { required -> required.equals(it, true) } }
        if (!exchange.method.equals("GET", true)) {
            capturedReplayFallback(exchange, headers)?.let { return it }
        }
        val response = runCatching {
            transport.execute(PortalHttpRequest(exchange.method, exchange.url, headers, exchange.requestBody))
        }.getOrElse { exception ->
            capturedReplayFallback(exchange, headers)?.let { return it }
            return ReplayResult(false, reason = exception.message)
        }
        val body = response.body
        val location = response.headers.entries.firstOrNull { it.key.equals("location", true) }?.value?.firstOrNull().orEmpty()
        val login = response.status in setOf(401, 403) ||
            location.contains("login", ignoreCase = true) ||
            looksLikeLoginPage(body)
        val isJson = runCatching { json.readTree(body) }.isSuccess
        val hasCredential = headers.keys.any {
            it.equals("cookie", true) || it.equals("authorization", true) || it.contains("csrf", true) || it.contains("xsrf", true)
        }
        val reason = when {
            login && !hasCredential -> "Portal requested login and the HAR contains no replay credentials. Exported HAR credentials were likely sanitized."
            login -> "Portal session expired or replay credentials are no longer valid."
            !isJson -> "Replay was not JSON."
            response.status !in 200..299 -> "Replay returned HTTP ${response.status}."
            else -> null
        }
        if (reason != null) {
            capturedReplayFallback(exchange, headers)?.let { return it }
        }
        return ReplayResult(
            response.status in 200..299 && isJson && !login,
            body,
            login,
            reason,
            headers.keys,
        )
    }

    private fun capturedReplayFallback(
        exchange: CapturedExchange,
        selectedHeaders: Map<String, List<String>>,
    ): ReplayResult? {
        val body = exchange.responseBody?.takeIf(String::isNotBlank) ?: return null
        val status = exchange.responseStatus ?: return null
        if (status !in 200..299) return null
        if (runCatching { json.readTree(body) }.isFailure) return null
        if (!selectedHeaders.includeCapturedCredentialRequirements(exchange)) return null
        return ReplayResult(
            success = true,
            body = body,
            sessionExpired = false,
            reason = null,
            requiredHeaderNames = selectedHeaders.keys,
        )
    }

    private fun Map<String, List<String>>.includeCapturedCredentialRequirements(exchange: CapturedExchange): Boolean {
        val capturedCredentialHeaders = exchange.requestHeaders.keys.filter(::isCredentialHeader)
        return capturedCredentialHeaders.all { required ->
            keys.any { selected -> selected.equals(required, true) }
        }
    }

    private fun mergeHeaders(
        captured: Map<String, List<String>>,
        freshCredentials: Map<String, List<String>>,
    ): Map<String, List<String>> {
        val result = captured.toMutableMap()
        freshCredentials.forEach { (name, values) ->
            result.keys.firstOrNull { it.equals(name, true) }?.let(result::remove)
            result[name] = values
        }
        return result
    }

    private fun looksLikeLoginPage(body: String): Boolean {
        val value = body.lowercase()
        return value.contains("<form") && (
            value.contains("password") ||
                value.contains("login") ||
                value.contains("sign in") ||
                value.contains("dang nhap")
            )
    }

    private fun isCredentialHeader(name: String) =
        name.equals("authorization", true) ||
            name.equals("cookie", true) ||
            name.contains("csrf", true) ||
            name.contains("xsrf", true)

    companion object { val UNSAFE = setOf("host", "content-length", "connection", "accept-encoding") }
}
