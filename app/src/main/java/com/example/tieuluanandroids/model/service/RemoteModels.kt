package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.sync.*


data class ApiResult(
    val success: Boolean,
    val message: String
)

data class LoginApiResult(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val session: SessionInfo? = null
)

data class RemoteEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: String,
    val endTime: String,
    val tagId: String?,
    val tagName: String,
    val ownerId: String?,
    val ownerName: String
)

data class RemoteTag(
    val id: String,
    val name: String,
    val color: String?,
    val ownerId: String?
)

data class RemoteListResult<T>(
    val success: Boolean,
    val message: String,
    val items: List<T>
)

data class RemoteWriteResult(
    val success: Boolean,
    val message: String,
    val remoteId: String? = null
)

data class AgentChatResult(
    val success: Boolean,
    val message: String,
    val response: AgentChatResponse? = null
)

data class AgentChatResponse(
    val answer: String,
    val toolCalls: List<AgentToolCall> = emptyList(),
    val needsConfirmation: Boolean = false,
    val needsClarification: Boolean = false,
    val pendingConfirmationId: String? = null,
    val missing: List<String> = emptyList()
)

data class AgentToolCall(
    val toolId: Int?,
    val toolName: String,
    val category: String,
    val params: Map<String, String> = emptyMap(),
    val upstreamStatus: Int?
)
