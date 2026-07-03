package com.example.tieuluanandroids.model

data class Tag(
    val localId: String,
    val remoteId: String?,
    val name: String,
    val color: String?,
    val ownerId: String?,
    val syncStatus: SyncStatus,
    val updatedAt: Long,
    val deletedAt: Long?
)

data class CreateTagInput(val name: String, val color: String? = null, val ownerId: String? = null)

data class UpdateTagInput(
    val localId: String,
    val name: String,
    val color: String? = null,
    val ownerId: String? = null
)
