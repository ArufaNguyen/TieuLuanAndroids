package com.example.smartcalendar.service

import com.example.smartcalendar.dto.reverseapi.ApiKnowledgeResponse
import com.example.smartcalendar.dto.reverseapi.UpsertGlobalApiKnowledgePresetRequest
import com.example.smartcalendar.dto.reverseapi.UpsertGlobalApiKnowledgeRequest
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.KnowledgeSource
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.portaldiscovery.knowledge.ApiParameter
import com.example.smartcalendar.portaldiscovery.knowledge.AuthType
import com.example.smartcalendar.portaldiscovery.knowledge.HeadersPolicy
import com.example.smartcalendar.portaldiscovery.knowledge.KnowledgeStatus
import com.example.smartcalendar.portaldiscovery.knowledge.SafetyLevel
import com.example.smartcalendar.repository.ApiKnowledgeJpaRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime

@Service
class ApiKnowledgeService(
    private val repository: ApiKnowledgeJpaRepository,
    private val json: ObjectMapper
) {
    fun getAll(userId: Int?): List<ApiKnowledgeResponse> =
        (userId?.let {
            repository.findAllByUserIsNullOrderByUpdatedAtDesc() + repository.findAllByUserIdOrderByUpdatedAtDesc(it)
        } ?: repository.findAllByUserIsNullOrderByUpdatedAtDesc())
            .sortedByDescending(ApiKnowledgeEntity::updatedAt)
            .map(::toResponse)

    fun getGlobal(): List<ApiKnowledgeResponse> =
        repository.findAllByUserIsNullOrderByUpdatedAtDesc().map(::toResponse)

    fun get(id: Int): ApiKnowledgeResponse =
        repository.findById(id).orElseThrow { ApiException(404, "API knowledge not found") }.let(::toResponse)

    fun upsertGlobal(request: UpsertGlobalApiKnowledgeRequest): ApiKnowledgeResponse {
        val node = request.knowledge ?: request.knowledgeJson?.let(json::readTree)
            ?: throw ApiException(400, "knowledge or knowledgeJson is required")
        val knowledge = json.treeToValue(node, ApiKnowledge::class.java)
        return saveGlobal(knowledge)
    }

    fun upsertGlobalPreset(request: UpsertGlobalApiKnowledgePresetRequest): ApiKnowledgeResponse {
        val method = request.method.trim().uppercase()
        if (method.isBlank()) throw ApiException(400, "method is required")
        val url = request.url.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            throw ApiException(400, "url must start with http:// or https://")
        }
        val category = runCatching { EndpointCategory.valueOf(request.category.trim().uppercase()) }
            .getOrElse { EndpointCategory.OTHER }
        val toolName = request.toolName?.trim()?.takeIf(String::isNotBlank)
            ?: defaultToolName(category, method, url)
        val urlParams = PLACEHOLDER.findAll(url).map { it.groupValues[1] }.toList()
        val requiredParams = (urlParams + request.params.map(String::trim).filter(String::isNotBlank))
            .distinct()
            .map { ApiParameter(it) }
        val headerNames = request.headers.keys.map(String::trim).filter(String::isNotBlank)
        val credentialHeaderNames = request.credentialHeaders.map(String::trim).filter(String::isNotBlank)
        val staticHeaderNames = headerNames.filterNot { header ->
            credentialHeaderNames.any { it.equals(header, ignoreCase = true) }
        }
        val now = Instant.now().toString()
        val knowledge = ApiKnowledge(
            toolName = toolName,
            purpose = "Global preset for $method $url",
            category = category,
            method = method,
            portalUrl = ApiKnowledge.inferPortalUrl(url),
            urlTemplate = url,
            authType = if (credentialHeaderNames.isEmpty()) AuthType.NONE else AuthType.SESSION_CREDENTIALS_AT_RUNTIME,
            requiredParams = requiredParams,
            headersPolicy = HeadersPolicy(
                requiredCredentialHeaders = credentialHeaderNames,
                requiredStaticHeaders = staticHeaderNames
            ),
            requestBodySchema = request.body?.takeUnless(JsonNode::isNull)?.toString(),
            safetyLevel = if (method == "GET" && category != EndpointCategory.LOGIN) SafetyLevel.READ_ONLY else SafetyLevel.USER_CONFIRM_REQUIRED,
            confidence = 1.0,
            status = KnowledgeStatus.VERIFIED,
            source = KnowledgeSource.MANUAL,
            lastVerifiedAt = now,
            createdAt = now,
            updatedAt = now
        )
        return saveGlobal(knowledge)
    }

    fun copyToGlobal(id: Int): ApiKnowledgeResponse {
        val entity = repository.findById(id).orElseThrow { ApiException(404, "API knowledge not found") }
        val knowledge = json.readValue(entity.knowledgeJson, ApiKnowledge::class.java)
        return saveGlobal(knowledge)
    }

    fun delete(id: Int): String {
        if (!repository.existsById(id)) throw ApiException(404, "API knowledge not found")
        repository.deleteById(id)
        return "API knowledge deleted successfully"
    }

    private fun saveGlobal(source: ApiKnowledge): ApiKnowledgeResponse {
        val candidate = source
            .copy(
                id = 0,
                toolName = source.toolName.stripUserPrefix(),
                portalUrl = source.portalUrl.ifBlank { ApiKnowledge.inferPortalUrl(source.urlTemplate) }
            )
        val naturalKey = candidate.naturalKey().sha256()
        val entity = repository.findByUserIsNullAndNaturalKey(naturalKey) ?: ApiKnowledgeEntity(
            user = null,
            naturalKey = naturalKey,
            createdAt = LocalDateTime.now()
        )
        entity.discoveryJobId = null
        entity.toolName = candidate.toolName
        entity.portalUrl = candidate.portalUrl
        entity.method = candidate.method
        entity.category = candidate.category.name
        entity.updatedAt = LocalDateTime.now()
        entity.knowledgeJson = json.writeValueAsString(candidate.copy(id = entity.id))
        val saved = repository.save(entity)
        saved.knowledgeJson = json.writeValueAsString(candidate.copy(id = saved.id))
        return toResponse(repository.save(saved))
    }

    private fun toResponse(entity: ApiKnowledgeEntity) = ApiKnowledgeResponse(
        id = entity.id,
        userId = entity.user?.id,
        scope = if (entity.user == null) "GLOBAL" else "CUSTOM",
        discoveryJobId = entity.discoveryJobId,
        toolName = entity.toolName,
        portalUrl = entity.portalUrl,
        method = entity.method,
        category = entity.category,
        knowledge = json.readTree(entity.knowledgeJson),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    private fun String.stripUserPrefix(): String =
        replace(Regex("""^user_\d+_"""), "")

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun defaultToolName(category: EndpointCategory, method: String, url: String): String {
        val path = runCatching { java.net.URI(url).path }.getOrNull().orEmpty()
        val tail = path.trim('/').split('/').lastOrNull()?.takeIf(String::isNotBlank) ?: category.name.lowercase()
        val slug = tail.replace(Regex("""[^A-Za-z0-9]+"""), "_").trim('_').lowercase()
        return if (category == EndpointCategory.OTHER) "${method.lowercase()}_$slug" else "get_${category.name.lowercase()}".takeIf { method == "GET" } ?: category.name.lowercase()
    }

    companion object {
        private val PLACEHOLDER = Regex("""\{([^}]+)}""")
    }
}
