package com.example.smartcalendar.dto.reverseapi

import com.example.smartcalendar.model.DiscoveryJobStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

data class DiscoveryJobResponse(
    val id: String,
    val userId: Int?,
    val fileName: String?,
    val status: DiscoveryJobStatus,
    val result: JsonNode?,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)

data class ApiKnowledgeResponse(
    val id: Int,
    val userId: Int?,
    val scope: String,
    val discoveryJobId: String?,
    val toolName: String,
    val portalUrl: String,
    val method: String,
    val category: String,
    val knowledge: JsonNode,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class RunKnownToolRequest(
    val params: Map<String, String> = emptyMap(),
    val credentials: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: JsonNode? = null
)

data class RunKnownToolResponse(
    val toolName: String,
    val resolvedToolName: String,
    val method: String,
    val url: String,
    val status: Int,
    val contentType: String?,
    val body: JsonNode?,
    val rawBody: String?,
    val usedHeaders: List<String>
)

data class UpsertGlobalApiKnowledgeRequest(
    val knowledge: JsonNode? = null,
    val knowledgeJson: String? = null
)

data class UpsertGlobalApiKnowledgePresetRequest(
    val toolName: String? = null,
    val category: String = "OTHER",
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val credentialHeaders: List<String> = emptyList(),
    val params: List<String> = emptyList(),
    val body: JsonNode? = null
)
