package com.example.smartcalendar.portaldiscovery.runtime

import com.example.smartcalendar.portaldiscovery.CalendarEventDto
import com.example.smartcalendar.portaldiscovery.core.CalendarNormalizer
import com.example.smartcalendar.portaldiscovery.core.PortalCredentialProvider
import com.example.smartcalendar.portaldiscovery.core.PortalHttpRequest
import com.example.smartcalendar.portaldiscovery.core.PortalHttpTransport
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.portaldiscovery.knowledge.KnowledgeStatus
import com.example.smartcalendar.portaldiscovery.knowledge.SafetyLevel
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class KnownApiCallResult(
    val success: Boolean,
    val status: KnownApiCallStatus,
    val httpStatus: Int? = null,
    val responseBody: String? = null,
    val events: List<CalendarEventDto> = emptyList(),
    val failureReason: String? = null,
)

enum class KnownApiCallStatus {
    SUCCESS,
    LOGIN_REQUIRED,
    INVALID_KNOWLEDGE,
    MISSING_PARAMETER,
    HTTP_FAILED,
    NON_JSON_RESPONSE,
    NORMALIZATION_FAILED,
}

class KnownApiCaller(
    private val json: ObjectMapper,
    private val transport: PortalHttpTransport,
    private val credentials: PortalCredentialProvider = PortalCredentialProvider.NONE,
) {
    fun call(knowledge: ApiKnowledge, parameters: Map<String, Any?> = emptyMap()): KnownApiCallResult {
        if (
            knowledge.status != KnowledgeStatus.VERIFIED ||
            knowledge.safetyLevel != SafetyLevel.READ_ONLY ||
            knowledge.method != "GET"
        ) {
            return failure(KnownApiCallStatus.INVALID_KNOWLEDGE, "Only verified read-only GET knowledge may run.")
        }
        val missing = knowledge.requiredParams.firstOrNull { it.required && parameters[it.name] == null }
        if (missing != null) {
            return failure(KnownApiCallStatus.MISSING_PARAMETER, "Missing required parameter: ${missing.name}.")
        }
        val url = PARAMETER.replace(knowledge.urlTemplate) { match ->
            encode(parameters[match.groupValues[1]]?.toString().orEmpty())
        }
        if (!url.startsWith(knowledge.portalUrl, ignoreCase = true)) {
            return failure(KnownApiCallStatus.INVALID_KNOWLEDGE, "URL template does not belong to the knowledge portal.")
        }
        val requiredHeaders = (
            knowledge.headersPolicy.requiredCredentialHeaders +
                knowledge.headersPolicy.requiredStaticHeaders
            ).toSet()
        val headers = credentials.headersFor(url)
            .filterKeys { it.lowercase() !in UNSAFE_HEADERS }
            .filterKeys { requiredHeaders.isEmpty() || requiredHeaders.any { required -> required.equals(it, true) } }
        val response = runCatching {
            transport.execute(PortalHttpRequest("GET", url, headers))
        }.getOrElse {
            return failure(KnownApiCallStatus.HTTP_FAILED, it.message ?: "Portal request failed.")
        }
        val location = response.headers.entries
            .firstOrNull { it.key.equals("location", true) }
            ?.value
            ?.firstOrNull()
            .orEmpty()
        if (
            response.status in setOf(401, 403) ||
            location.contains("login", true) ||
            looksLikeLoginPage(response.body)
        ) {
            return KnownApiCallResult(
                false,
                KnownApiCallStatus.LOGIN_REQUIRED,
                response.status,
                failureReason = "Portal session is missing or expired. User login is required.",
            )
        }
        if (response.status !in 200..299) {
            return KnownApiCallResult(
                false,
                KnownApiCallStatus.HTTP_FAILED,
                response.status,
                failureReason = "Portal returned HTTP ${response.status}.",
            )
        }
        if (runCatching { json.readTree(response.body) }.isFailure) {
            return KnownApiCallResult(
                false,
                KnownApiCallStatus.NON_JSON_RESPONSE,
                response.status,
                failureReason = "Portal response was not JSON.",
            )
        }
        val mapping = knowledge.responseMapping
        if (mapping == null) {
            return KnownApiCallResult(true, KnownApiCallStatus.SUCCESS, response.status, response.body)
        }
        val events = CalendarNormalizer(json).normalize(response.body, mapping)
        if (events.isEmpty()) {
            return KnownApiCallResult(
                false,
                KnownApiCallStatus.NORMALIZATION_FAILED,
                response.status,
                response.body,
                failureReason = "Verified response mapping produced no events.",
            )
        }
        return KnownApiCallResult(true, KnownApiCallStatus.SUCCESS, response.status, response.body, events)
    }

    private fun failure(status: KnownApiCallStatus, reason: String) =
        KnownApiCallResult(false, status, failureReason = reason)

    private fun looksLikeLoginPage(body: String): Boolean {
        val value = body.lowercase()
        return value.contains("<form") && (value.contains("password") || value.contains("login") || value.contains("dang nhap"))
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    companion object {
        private val PARAMETER = Regex("""\{([^}]+)}""")
        private val UNSAFE_HEADERS = setOf("host", "content-length", "connection", "accept-encoding")
    }
}
