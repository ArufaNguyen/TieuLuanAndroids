package com.example.smartcalendar.portaldiscovery.knowledge

import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.KnowledgeSource

data class ApiKnowledge(
    val id: Int = 0,
    val toolName: String,
    val purpose: String,
    val category: EndpointCategory,
    val method: String,
    val portalUrl: String = "",
    val urlTemplate: String,
    val authType: AuthType,
    val requiredParams: List<ApiParameter>,
    val optionalParams: List<ApiParameter> = emptyList(),
    val headersPolicy: HeadersPolicy = HeadersPolicy(),
    val requestBodySchema: String? = null,
    val responseSchema: String? = null,
    val responseMapping: CalendarMapping? = null,
    val safetyLevel: SafetyLevel,
    val confidence: Double,
    val status: KnowledgeStatus,
    val source: KnowledgeSource,
    val lastVerifiedAt: String,
    val createdAt: String,
    val updatedAt: String,
) {
    fun naturalKey(): String = "$portalUrl|$toolName|$method|$urlTemplate"

    fun withInferredPortalUrl(): ApiKnowledge =
        if (portalUrl.isNotBlank()) this else copy(portalUrl = inferPortalUrl(urlTemplate))

    companion object {
        fun inferPortalUrl(url: String): String =
            ORIGIN.find(url)?.groupValues?.get(1)?.trimEnd('/') ?: url.substringBefore('/')

        private val ORIGIN = Regex("""^(https?://[^/]+)""", RegexOption.IGNORE_CASE)
    }
}

data class ApiParameter(val name: String, val required: Boolean = true)

data class HeadersPolicy(
    val credentialSource: String = "PortalCredentialProvider",
    val requiredCredentialHeaders: List<String> = emptyList(),
    val requiredStaticHeaders: List<String> = emptyList(),
    val persistedHeaders: List<String> = emptyList(),
    val droppedHeaders: List<String> = listOf("authorization", "cookie", "set-cookie", "x-csrf-token", "x-xsrf-token"),
)

enum class AuthType { SESSION_CREDENTIALS_AT_RUNTIME, NONE }
enum class KnowledgeStatus { CANDIDATE, VERIFIED, BROKEN, DEPRECATED, IGNORED }
enum class SafetyLevel { READ_ONLY, USER_CONFIRM_REQUIRED, DANGEROUS_BLOCKED }
