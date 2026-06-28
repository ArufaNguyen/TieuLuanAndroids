package com.example.tieuluanandroids.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tieuluanandroids.data.model.SyncStatus

@Entity(tableName = "tags", indices = [Index(value = ["remoteId"], unique = true)])
data class TagEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val name: String,
    val color: String?,
    val ownerId: String?,
    val syncStatus: SyncStatus,
    val updatedAt: Long,
    val deletedAt: Long?
)
