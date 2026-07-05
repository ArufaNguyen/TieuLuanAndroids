package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.model.PortalCredential
import com.example.smartcalendar.model.User
import com.example.smartcalendar.repository.PortalCredentialRepository
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class LoginCredentialPersistService(
    private val repository: PortalCredentialRepository
) {
    private val log = LoggerFactory.getLogger(LoginCredentialPersistService::class.java)

    fun persistIfLogin(user: User, tool: AgentToolDescriptor, runResult: KnownToolRunResult): Boolean {
        if (!tool.category.equals("LOGIN", ignoreCase = true) || runResult.status !in 200..299) return false
        val token = findToken(runResult.body) ?: findTokenInText(runResult.rawBody)
        if (token.isNullOrBlank()) {
            log.info("login_credential_not_persisted userId={} toolId={} reason=no_token_found", user.id, tool.id)
            return false
        }
        val credential = repository.findFirstByUserIdOrderByLastCapturedAtDescIdDesc(user.id) ?: PortalCredential(user = user)
        credential.loginToolId = tool.id
        credential.portalUrl = runResult.url.substringBefore("/api/", runResult.url).trimEnd('/')
        credential.loginUrlTemplate = runResult.url
        credential.authorization = normalizeAuthorization(token)
        credential.lastCapturedAt = Instant.now()
        repository.save(credential)
        log.info("login_credential_persisted userId={} toolId={}", user.id, tool.id)
        return true
    }

    private fun findToken(node: JsonNode?): String? {
        if (node == null || node.isNull || node.isMissingNode) return null
        val direct = TOKEN_FIELD_NAMES.firstNotNullOfOrNull { name ->
            node.path(name).takeIf { it.isTextual }?.asText()?.takeIf(String::isNotBlank)
        }
        if (!direct.isNullOrBlank()) return direct
        if (node.isObject || node.isArray) {
            val children = node.elements()
            while (children.hasNext()) {
                val child = children.next()
                val nested = findToken(child)
                if (!nested.isNullOrBlank()) return nested
            }
        }
        return null
    }

    private fun findTokenInText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return JWT.find(raw)?.value
    }

    private fun normalizeAuthorization(value: String): String =
        value.trim().let { if (it.startsWith("Bearer ", ignoreCase = true)) it else "Bearer $it" }

    companion object {
        private val TOKEN_FIELD_NAMES = listOf(
            "token",
            "accessToken",
            "access_token",
            "jwt",
            "jwtToken",
            "idToken",
            "id_token"
        )
        private val JWT = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
    }
}
