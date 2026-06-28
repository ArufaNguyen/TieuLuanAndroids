package com.example.tieuluanandroids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tieuluanandroids.data.model.SyncStatus

@Entity(tableName = "events", indices = [Index(value = ["remoteId"], unique = true)])
data class EventEntity(
    @PrimaryKey val localId: String,
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
