package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE deletedAt IS NULL AND ownerId = :ownerId ORDER BY name")
    fun observeActive(ownerId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): TagEntity?

    @Query("SELECT * FROM tags WHERE localId = :localId AND ownerId = :ownerId LIMIT 1")
    suspend fun getByLocalIdForOwner(localId: String, ownerId: String): TagEntity?

    @Query("SELECT * FROM tags WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TagEntity?

    @Upsert
    suspend fun upsert(entity: TagEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TagEntity>)

    @Query(
        "UPDATE tags SET remoteId = :remoteId, syncStatus = :status, " +
            "updatedAt = :updatedAt, deletedAt = :deletedAt WHERE localId = :localId"
    )
    suspend fun updateSyncState(
        localId: String,
        remoteId: String?,
        status: SyncStatus,
        updatedAt: Long,
        deletedAt: Long?
    )

    @Query("DELETE FROM tags WHERE localId = :localId")
    suspend fun hardDelete(localId: String)
}
