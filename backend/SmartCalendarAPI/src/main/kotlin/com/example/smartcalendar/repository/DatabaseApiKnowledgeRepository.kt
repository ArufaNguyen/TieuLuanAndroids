package com.example.smartcalendar.repository

import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.model.DiscoveryJob
import com.example.smartcalendar.model.User
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledgeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime

class DatabaseApiKnowledgeRepository(
    private val repository: ApiKnowledgeJpaRepository,
    private val json: ObjectMapper,
    private val user: User?,
    private val job: DiscoveryJob
) : ApiKnowledgeRepository {

    override fun saveAll(knowledge: List<ApiKnowledge>): List<ApiKnowledge> = knowledge.map { candidate ->
        val scopedCandidate = candidate.withUserScopedToolName()
        val naturalKey = scopedCandidate.naturalKey().sha256()
        val entity = findByNaturalKey(naturalKey) ?: ApiKnowledgeEntity(
            user = user,
            discoveryJobId = job.id,
            naturalKey = naturalKey,
            createdAt = LocalDateTime.now()
        )
        entity.discoveryJobId = job.id
        entity.toolName = scopedCandidate.toolName
        entity.portalUrl = scopedCandidate.portalUrl
        entity.method = scopedCandidate.method
        entity.category = scopedCandidate.category.name
        entity.updatedAt = LocalDateTime.now()
        entity.knowledgeJson = json.writeValueAsString(scopedCandidate.copy(id = entity.id))
        val saved = repository.save(entity)
        saved.knowledgeJson = json.writeValueAsString(scopedCandidate.copy(id = saved.id))
        repository.save(saved)
        scopedCandidate.copy(id = saved.id)
    }

    override fun findAll(): List<ApiKnowledge> = entities().mapNotNull { entity ->
        runCatching { json.readValue(entity.knowledgeJson, ApiKnowledge::class.java).copy(id = entity.id) }.getOrNull()
    }

    private fun findByNaturalKey(naturalKey: String): ApiKnowledgeEntity? =
        user?.let { repository.findByUserIdAndNaturalKey(it.id, naturalKey) }
            ?: repository.findByUserIsNullAndNaturalKey(naturalKey)

    private fun entities(): List<ApiKnowledgeEntity> =
        user?.let { repository.findAllByUserIdOrderByUpdatedAtDesc(it.id) }
            ?: repository.findAllByUserIsNullOrderByUpdatedAtDesc()

    private fun ApiKnowledge.withUserScopedToolName(): ApiKnowledge {
        val owner = user ?: return this
        val prefix = "user_${owner.id}_"
        return if (toolName.startsWith(prefix)) this else copy(toolName = "$prefix$toolName")
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

@Component
class DatabaseApiKnowledgeRepositoryFactory(
    private val repository: ApiKnowledgeJpaRepository,
    private val json: ObjectMapper
) {
    fun create(user: User?, job: DiscoveryJob): ApiKnowledgeRepository =
        DatabaseApiKnowledgeRepository(repository, json, user, job)
}
