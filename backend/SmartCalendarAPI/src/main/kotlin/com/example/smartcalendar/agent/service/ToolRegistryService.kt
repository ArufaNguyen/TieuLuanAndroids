package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.ToolExample
import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.repository.ApiKnowledgeJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class ToolRegistryService(
    private val repository: ApiKnowledgeJpaRepository,
    private val json: ObjectMapper
) {
    fun getToolsForUser(userId: Int): List<AgentToolDescriptor> =
        (repository.findAllByUserIsNullOrderByUpdatedAtDesc() + repository.findAllByUserIdOrderByUpdatedAtDesc(userId))
            .sortedByDescending(ApiKnowledgeEntity::updatedAt)
            .mapNotNull(::toDescriptor)

    private fun toDescriptor(entity: ApiKnowledgeEntity): AgentToolDescriptor? {
        val knowledge = runCatching { json.readValue(entity.knowledgeJson, ApiKnowledge::class.java) }.getOrNull()
            ?: return null
        val url = knowledge.urlTemplate.ifBlank { entity.portalUrl }
        val requiredParams = if (knowledge.requiredParams.isNotEmpty()) {
            knowledge.requiredParams.map { it.name }
        } else {
            URL_PARAM.findAll(url).map { it.groupValues[1] }.toList()
        }
        val safetyLevel = knowledge.safetyLevel.name
        return AgentToolDescriptor(
            id = entity.id,
            toolName = entity.toolName,
            scope = if (entity.user == null) "GLOBAL" else "CUSTOM",
            category = entity.category,
            method = entity.method.uppercase(),
            urlTemplate = url,
            description = knowledge.purpose,
            requiredParams = requiredParams,
            optionalParams = knowledge.optionalParams.map { it.name },
            requiredCredentialHeaders = knowledge.headersPolicy.requiredCredentialHeaders,
            optionalCredentialHeaders = knowledge.headersPolicy.requiredStaticHeaders,
            bodySchema = knowledge.requestBodySchema?.let { runCatching { json.readTree(it) }.getOrNull() },
            safetyLevel = safetyLevel,
            readOnly = safetyLevel == "READ_ONLY",
            examples = examples(entity.category, requiredParams)
        )
    }

    private fun examples(category: String, params: List<String>): List<ToolExample> = when (category.uppercase()) {
        "SCHEDULE" -> listOf(ToolExample("hôm nay tôi học gì", mapOf("date" to "today")))
        "SEMESTER" -> listOf(ToolExample("có học kỳ nào"))
        "NOTIFICATION" -> listOf(ToolExample("lấy danh mục thông báo"))
        "REGISTERED_COURSES" -> listOf(ToolExample("tôi đã đăng ký môn nào đợt 76", mapOf("idDot" to "76")))
        "RETAKE_COURSES" -> listOf(ToolExample("có môn cải thiện nào đợt 76 không", mapOf("idDot" to "76")))
        "LOGIN" -> listOf(ToolExample("đăng nhập portal"))
        else -> if (params.isEmpty()) emptyList() else listOf(ToolExample("chạy ${category.lowercase()}"))
    }

    companion object {
        private val URL_PARAM = Regex("""\{([^}]+)}""")
    }
}

