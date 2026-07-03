package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
