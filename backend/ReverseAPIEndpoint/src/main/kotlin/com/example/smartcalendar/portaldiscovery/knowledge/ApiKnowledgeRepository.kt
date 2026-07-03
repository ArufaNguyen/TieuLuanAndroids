package com.example.smartcalendar.portaldiscovery.knowledge

interface ApiKnowledgeRepository {
    fun saveAll(knowledge: List<ApiKnowledge>): List<ApiKnowledge>
    fun findAll(): List<ApiKnowledge>
    fun findByToolName(toolName: String): ApiKnowledge? = findAll().firstOrNull { it.toolName == toolName }
    fun findByPortalUrl(portalUrl: String): List<ApiKnowledge> =
        findAll().filter { it.portalUrl.equals(portalUrl.trimEnd('/'), ignoreCase = true) }

    fun findByPortalAndTool(portalUrl: String, toolName: String): ApiKnowledge? =
        findByPortalUrl(portalUrl).firstOrNull { it.toolName == toolName }

    companion object {
        val NONE = object : ApiKnowledgeRepository {
            override fun saveAll(knowledge: List<ApiKnowledge>) = emptyList<ApiKnowledge>()
            override fun findAll() = emptyList<ApiKnowledge>()
        }
    }
}
