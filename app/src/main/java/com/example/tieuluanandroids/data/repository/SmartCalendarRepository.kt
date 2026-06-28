package com.example.tieuluanandroids.data.repository

import com.example.tieuluanandroids.data.api.ApiResult
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.DiscoveryJob
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.model.Tag
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import kotlinx.coroutines.flow.Flow

interface SmartCalendarRepository {
    val isDevMode: Boolean

    fun observeEvents(): Flow<List<Event>>
    fun observeTags(): Flow<List<Tag>>
    fun observeSession(): Flow<SessionInfo?>

    suspend fun createEvent(input: CreateEventInput): AppResult<Unit>
    suspend fun updateEvent(input: UpdateEventInput): AppResult<Unit>
    suspend fun deleteEvent(localId: String): AppResult<Unit>
    suspend fun createTag(input: CreateTagInput): AppResult<Unit>
    suspend fun updateTag(input: UpdateTagInput): AppResult<Unit>
    suspend fun deleteTag(localId: String): AppResult<Unit>
    suspend fun syncNow(): AppResult<Unit>

    suspend fun login(username: String, password: String): ApiResult
    suspend fun enableDevMode()
    suspend fun checkBackendDevMode(): ApiResult
    suspend fun uploadHar(fileName: String, bytes: ByteArray): ApiResult
    suspend fun clearSession()
    suspend fun getDiscoveryJobs(): AppResult<List<DiscoveryJob>>


}
