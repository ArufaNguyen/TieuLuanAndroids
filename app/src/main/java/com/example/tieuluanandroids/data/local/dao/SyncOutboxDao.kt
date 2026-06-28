package com.example.tieuluanandroids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE ownerId = :ownerId ORDER BY createdAt, id")
    suspend fun getPending(ownerId: String): List<SyncOutboxEntity>

    @Query(
        "SELECT * FROM sync_outbox WHERE entityType = :entityType " +
            "AND entityLocalId = :localId ORDER BY createdAt LIMIT 1"
    )
    suspend fun findForEntity(entityType: String, localId: String): SyncOutboxEntity?

    @Upsert
    suspend fun upsert(entity: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_outbox WHERE entityType = :entityType AND entityLocalId = :localId")
    suspend fun deleteForEntity(entityType: String, localId: String)

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)
}
