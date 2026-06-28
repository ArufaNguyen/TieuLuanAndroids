package com.example.tieuluanandroids.data.api

import com.example.tieuluanandroids.data.model.SessionInfo

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
