package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.CompletePortalCredentialCaptureRequest
import com.example.smartcalendar.agent.dto.PortalCredentialCaptureResponse
import com.example.smartcalendar.agent.dto.StartPortalCredentialCaptureRequest
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.PortalCredential
import com.example.smartcalendar.model.User
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.repository.ApiKnowledgeJpaRepository
import com.example.smartcalendar.repository.PortalCredentialRepository
import com.example.smartcalendar.service.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PortalCredentialCaptureService(
    private val authService: AuthService,
    private val apiKnowledgeRepository: ApiKnowledgeJpaRepository,
    private val credentialRepository: PortalCredentialRepository,
    private val json: ObjectMapper
) {
    fun start(sessionToken: String?, request: StartPortalCredentialCaptureRequest): PortalCredentialCaptureResponse {
        val user = requireUser(sessionToken)
        val loginTool = request.loginToolId?.let { id ->
            apiKnowledgeRepository.findById(id).orElseThrow { ApiException(404, "login tool not found") }
                .also { entity ->
                    val ownerId = entity.user?.id
                    if (ownerId != null && ownerId != user.id) throw ApiException(403, "login tool does not belong to active session")
                    if (!entity.category.equals("LOGIN", ignoreCase = true)) throw ApiException(400, "tool is not a LOGIN tool")
                }
        } ?: apiKnowledgeRepository.findFirstByUserIdAndToolNameOrderByUpdatedAtDescIdDesc(user.id, "user_${user.id}_login")
            ?: apiKnowledgeRepository.findFirstByUserIdAndToolNameOrderByUpdatedAtDescIdDesc(user.id, "login")
            ?: apiKnowledgeRepository.findFirstByUserIsNullAndToolNameOrderByUpdatedAtDescIdDesc("login")

        val knowledge = loginTool?.let { runCatching { json.readValue(it.knowledgeJson, ApiKnowledge::class.java) }.getOrNull() }
        val capture = credentialRepository.save(
            PortalCredential(
                user = user,
                loginToolId = loginTool?.id,
                portalUrl = knowledge?.portalUrl ?: loginTool?.portalUrl,
                loginUrlTemplate = knowledge?.urlTemplate
            )
        )
        return toResponse(capture)
    }

    fun complete(
        sessionToken: String?,
        captureId: String,
        request: CompletePortalCredentialCaptureRequest
    ): PortalCredentialCaptureResponse {
        val user = requireUser(sessionToken)
        val capture = credentialRepository.findByCaptureId(captureId)
            ?: throw ApiException(404, "credential capture session not found")
        if (capture.user?.id != user.id) throw ApiException(403, "credential capture session does not belong to active session")
        if (request.authorization.isNullOrBlank() && request.cookie.isNullOrBlank() && request.csrfToken.isNullOrBlank()) {
            throw ApiException(400, "at least one credential value is required")
        }
        capture.authorization = request.authorization?.normalizeAuthorization()
        capture.cookie = request.cookie?.takeIf(String::isNotBlank)
        capture.csrfToken = request.csrfToken?.takeIf(String::isNotBlank)
        capture.expiresAt = request.expiresAt
        capture.lastCapturedAt = Instant.now()
        return toResponse(credentialRepository.save(capture))
    }

    fun current(sessionToken: String?): PortalCredentialCaptureResponse? {
        val user = requireUser(sessionToken)
        return credentialRepository.findFirstByUserIdOrderByLastCapturedAtDescIdDesc(user.id)?.let(::toResponse)
    }

    private fun requireUser(sessionToken: String?): User =
        (authService.getValidSession(sessionToken)
            ?: throw ApiException(401, "valid session is required for portal credential capture"))
            .account?.user ?: throw ApiException(401, "valid session is required for portal credential capture")

    private fun toResponse(capture: PortalCredential) = PortalCredentialCaptureResponse(
        captureId = capture.captureId,
        loginToolId = capture.loginToolId,
        portalUrl = capture.portalUrl,
        loginUrlTemplate = capture.loginUrlTemplate,
        requiredHeaders = listOf("Authorization", "Cookie", "X-CSRF-Token"),
        hasAuthorization = !capture.authorization.isNullOrBlank(),
        hasCookie = !capture.cookie.isNullOrBlank(),
        hasCsrfToken = !capture.csrfToken.isNullOrBlank(),
        expiresAt = capture.expiresAt,
        lastCapturedAt = capture.lastCapturedAt
    )

    private fun String.normalizeAuthorization(): String =
        trim().let { if (it.startsWith("Bearer ", ignoreCase = true)) it else "Bearer $it" }
}

