package com.example.smartcalendar.portaldiscovery.knowledge

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class JsonFileApiKnowledgeRepository(
    private val path: Path,
    private val json: ObjectMapper,
) : ApiKnowledgeRepository {
    @Synchronized
    override fun saveAll(knowledge: List<ApiKnowledge>): List<ApiKnowledge> {
        if (knowledge.isEmpty()) return emptyList()
        val existing = findAll().associateBy(ApiKnowledge::naturalKey).toMutableMap()
        var nextId = (existing.values.maxOfOrNull(ApiKnowledge::id) ?: 0) + 1
        val saved = mutableListOf<ApiKnowledge>()
        knowledge.forEach { candidate ->
            val key = candidate.naturalKey()
            val previous = existing[key]
            val value = candidate.copy(
                id = previous?.id ?: nextId++,
                createdAt = previous?.createdAt ?: candidate.createdAt,
            )
            existing[key] = value
            saved += value
        }
        Files.createDirectories(path.toAbsolutePath().parent)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), existing.values.sortedBy(ApiKnowledge::id))
        runCatching {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
        return saved
    }

    @Synchronized
    override fun findAll(): List<ApiKnowledge> {
        if (!Files.isRegularFile(path)) return emptyList()
        return runCatching {
            json.readValue(path.toFile(), object : TypeReference<List<ApiKnowledge>>() {})
        }.getOrElse {
            readLegacyUuidKnowledge()
        }.map(ApiKnowledge::withInferredPortalUrl)
    }

    private fun readLegacyUuidKnowledge(): List<ApiKnowledge> {
        val entries = json.readTree(path.toFile())
        if (!entries.isArray) return emptyList()
        return entries.mapIndexed { index, node ->
            val copy = node.deepCopy<ObjectNode>()
            copy.put("id", index + 1)
            json.treeToValue(copy, ApiKnowledge::class.java).withInferredPortalUrl()
        }
    }
}
