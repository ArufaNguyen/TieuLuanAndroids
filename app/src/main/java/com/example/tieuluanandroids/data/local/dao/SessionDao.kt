package com.example.tieuluanandroids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.tieuluanandroids.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM active_session WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SessionEntity?>

    @Query("SELECT * FROM active_session WHERE id = 1 LIMIT 1")
    suspend fun get(): SessionEntity?

    @Upsert
    suspend fun save(session: SessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clear()
}
