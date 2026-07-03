package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
