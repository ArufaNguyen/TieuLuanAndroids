package com.example.tieuluanandroids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tieuluanandroids.data.local.entity.EventEntity
import com.example.tieuluanandroids.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query(
        "SELECT * FROM events WHERE deletedAt IS NULL AND ownerId = :ownerId " +
            "ORDER BY startTime, title"
    )
    fun observeActive(ownerId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): EventEntity?

    @Query("SELECT * FROM events WHERE localId = :localId AND ownerId = :ownerId LIMIT 1")
    suspend fun getByLocalIdForOwner(localId: String, ownerId: String): EventEntity?

    @Query("SELECT * FROM events WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): EventEntity?

    @Upsert
    suspend fun upsert(entity: EventEntity)

    @Upsert
    suspend fun upsertAll(entities: List<EventEntity>)

    @Query(
        "UPDATE events SET remoteId = :remoteId, syncStatus = :status, " +
            "updatedAt = :updatedAt, deletedAt = :deletedAt WHERE localId = :localId"
    )
    suspend fun updateSyncState(
        localId: String,
        remoteId: String?,
        status: SyncStatus,
        updatedAt: Long,
        deletedAt: Long?
    )

    @Query("DELETE FROM events WHERE localId = :localId")
    suspend fun hardDelete(localId: String)
}
