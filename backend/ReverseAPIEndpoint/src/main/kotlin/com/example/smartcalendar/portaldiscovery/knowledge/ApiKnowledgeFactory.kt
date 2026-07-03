package com.example.smartcalendar.portaldiscovery.knowledge

import com.example.smartcalendar.portaldiscovery.DiscoveryRequest
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.ToolDefinition
import com.example.smartcalendar.portaldiscovery.VerifiedEndpoint
import java.time.Instant

class ApiKnowledgeFactory {
    fun create(
        tools: List<ToolDefinition>,
        verifiedEndpoints: List<VerifiedEndpoint>,
        request: DiscoveryRequest,
        now: Instant = Instant.now(),
    ): List<ApiKnowledge> {
        val verifiedById = verifiedEndpoints.associateBy(VerifiedEndpoint::endpointId)
        val timestamp = now.toString()
        return tools.mapNotNull { tool ->
            val endpoint = verifiedById[tool.endpointId] ?: return@mapNotNull null
            if (endpoint.method != "GET" && tool.category != EndpointCategory.LOGIN) return@mapNotNull null
            ApiKnowledge(
                toolName = tool.toolName,
                purpose = purpose(tool.category),
                category = tool.category,
                method = endpoint.method,
                portalUrl = ApiKnowledge.inferPortalUrl(endpoint.urlTemplate),
                urlTemplate = endpoint.urlTemplate,
                authType = if (tool.category == EndpointCategory.LOGIN) AuthType.NONE else AuthType.SESSION_CREDENTIALS_AT_RUNTIME,
                requiredParams = PARAMETER.findAll(endpoint.urlTemplate).map { ApiParameter(it.groupValues[1]) }.toList(),
                headersPolicy = HeadersPolicy(
                    requiredCredentialHeaders = endpoint.requiredCredentialHeaders.sorted(),
                    requiredStaticHeaders = endpoint.requiredStaticHeaders.sorted(),
                ),
                responseMapping = tool.mapping,
                safetyLevel = if (tool.category == EndpointCategory.LOGIN) SafetyLevel.USER_CONFIRM_REQUIRED else SafetyLevel.READ_ONLY,
                confidence = endpoint.confidence,
                status = KnowledgeStatus.VERIFIED,
                source = request.source,
                lastVerifiedAt = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }

    private fun purpose(category: EndpointCategory) = when (category) {
        EndpointCategory.LOGIN -> "Login to the portal and obtain session credentials."
        EndpointCategory.SCHEDULE -> "Get the student's schedule."
        EndpointCategory.REGISTERED_COURSES -> "Get courses already registered by the student."
        EndpointCategory.AVAILABLE_COURSES -> "Get courses currently available for registration."
        EndpointCategory.RETAKE_COURSES -> "Get courses available for retake or grade improvement."
        EndpointCategory.NOTIFICATION -> "Get portal notifications."
        EndpointCategory.SEMESTER -> "Get portal semesters."
        else -> "Read verified portal data."
    }

    companion object {
        private val PARAMETER = Regex("""\{([^}]+)}""")
    }
}
