package com.example.tieuluanandroids.data.repository

import com.example.tieuluanandroids.data.api.ApiResult
import com.example.tieuluanandroids.data.api.RemoteDataSource
import com.example.tieuluanandroids.data.api.RemoteEvent
import com.example.tieuluanandroids.data.api.RemoteListResult
import com.example.tieuluanandroids.data.api.RemoteTag
import com.example.tieuluanandroids.data.api.RemoteWriteResult
import com.example.tieuluanandroids.data.local.LocalDataSource
import com.example.tieuluanandroids.data.local.entity.EventEntity
import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity
import com.example.tieuluanandroids.data.local.entity.TagEntity
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.session.SessionCredentials
import com.example.tieuluanandroids.data.session.SessionManager
import com.example.tieuluanandroids.data.sync.SyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSmartCalendarRepositoryTest {
    @Test
    fun `create persists locally before scheduling sync`() = runTest {
        val calls = mutableListOf<String>()
        val repository = DefaultSmartCalendarRepository(
            localDataSource = RecordingLocalDataSource(calls),
            remoteDataSource = NoOpRemoteDataSource(),
            syncCoordinator = RecordingSyncCoordinator(calls),
            sessionManager = NoOpSessionManager()
        )

        val result = repository.createEvent(
            CreateEventInput(
                title = "Offline event",
                startTime = "2026-01-01T08:00:00",
                endTime = "2026-01-01T09:00:00"
            )
        )

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("local", "enqueue"), calls)
    }
}

private class NoOpSessionManager : SessionManager {
    override fun observeSession(): Flow<SessionInfo?> = flowOf(null)
    override suspend fun getCredentials(): SessionCredentials = SessionCredentials("token", 1)
    override suspend fun save(token: String, session: SessionInfo) = Unit
    override suspend fun clear() = Unit
}

private class RecordingLocalDataSource(private val calls: MutableList<String>) : LocalDataSource {
    override fun observeEvents(ownerId: String): Flow<List<EventEntity>> = flowOf(emptyList())
    override fun observeTags(ownerId: String): Flow<List<TagEntity>> = flowOf(emptyList())
    override suspend fun createEvent(input: CreateEventInput) { calls += "local" }
    override suspend fun updateEvent(input: UpdateEventInput) = true
    override suspend fun deleteEvent(localId: String, ownerId: String) = true
    override suspend fun createTag(input: CreateTagInput) = Unit
    override suspend fun updateTag(input: UpdateTagInput) = true
    override suspend fun deleteTag(localId: String, ownerId: String) = true
    override suspend fun pendingChanges(ownerId: String): List<SyncOutboxEntity> = emptyList()
    override suspend fun markSyncSuccess(change: SyncOutboxEntity, remoteId: String?) = Unit
    override suspend fun markSyncFailure(change: SyncOutboxEntity, error: String) = Unit
    override suspend fun mergeRemoteEvents(remoteEvents: List<RemoteEvent>) = Unit
    override suspend fun mergeRemoteTags(remoteTags: List<RemoteTag>) = Unit
}

private class RecordingSyncCoordinator(private val calls: MutableList<String>) : SyncCoordinator {
    override fun enqueue() { calls += "enqueue" }
    override suspend fun syncNow(): AppResult<Unit> = AppResult.Success(Unit)
}

private class NoOpRemoteDataSource : RemoteDataSource {
    override val isDevMode = false
    override suspend fun login(username: String, password: String) = ApiResult(true, "OK")
    override suspend fun enableDevMode() = Unit
    override suspend fun checkBackendDevMode() = ApiResult(true, "OK")
    override suspend fun uploadHar(fileName: String, bytes: ByteArray) = ApiResult(true, "OK")
    override suspend fun pullEvents() = RemoteListResult<RemoteEvent>(true, "OK", emptyList())
    override suspend fun pullTags() = RemoteListResult<RemoteTag>(true, "OK", emptyList())
    override suspend fun push(change: SyncOutboxEntity) = RemoteWriteResult(true, "OK")
}
