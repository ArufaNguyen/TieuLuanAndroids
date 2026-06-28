package com.example.tieuluanandroids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["entityType", "entityLocalId"]),
        Index(value = ["ownerId"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityLocalId: String,
    val ownerId: String?,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
