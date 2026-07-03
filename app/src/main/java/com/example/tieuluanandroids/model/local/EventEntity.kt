package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
