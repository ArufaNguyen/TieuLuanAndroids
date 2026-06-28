package com.example.tieuluanandroids.data.model

data class Event(
    val localId: String,
    val remoteId: String?,
    val title: String,
    val description: String?,
    val startTime: String,
    val endTime: String,
    val tagLocalId: String?,
    val tagName: String,
    val ownerId: String?,
    val ownerName: String,
    val syncStatus: SyncStatus,
    val updatedAt: Long,
    val deletedAt: Long?
)

data class CreateEventInput(
    val title: String,
    val description: String? = null,
    val startTime: String,
    val endTime: String,
    val tagLocalId: String? = null,
    val ownerId: String? = null
)

data class UpdateEventInput(
    val localId: String,
    val title: String,
    val description: String? = null,
    val startTime: String,
    val endTime: String,
    val tagLocalId: String? = null,
    val ownerId: String? = null
)
