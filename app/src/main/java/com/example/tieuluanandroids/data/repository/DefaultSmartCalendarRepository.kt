package com.example.tieuluanandroids.data.repository

import com.example.tieuluanandroids.data.api.RemoteDataSource
import com.example.tieuluanandroids.data.local.LocalDataSource
import com.example.tieuluanandroids.data.local.entity.EventEntity
import com.example.tieuluanandroids.data.local.entity.TagEntity
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.DiscoveryJob
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.model.Tag
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import com.example.tieuluanandroids.data.session.SessionManager
import com.example.tieuluanandroids.data.sync.SyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DefaultSmartCalendarRepository(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val sessionManager: SessionManager
) : SmartCalendarRepository {
    override val isDevMode: Boolean
        get() = remoteDataSource.isDevMode

    override fun observeEvents(): Flow<List<Event>> {
        return sessionManager.observeSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                observeEventsForUser(session.userId)
            }
        }
    }

    override fun observeTags(): Flow<List<Tag>> {
        return sessionManager.observeSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                observeTagsForUser(session.userId)
            }
        }
    }

    override fun observeSession(): Flow<SessionInfo?> = sessionManager.observeSession()

    override suspend fun createEvent(input: CreateEventInput): AppResult<Unit> {
        return writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            localDataSource.createEvent(ownedInput)
        }
    }

    override suspend fun updateEvent(input: UpdateEventInput): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            check(localDataSource.updateEvent(ownedInput)) {
                "Event not found: ${input.localId}"
            }
        }

    override suspend fun deleteEvent(localId: String): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            check(localDataSource.deleteEvent(localId, ownerId)) { "Event not found: $localId" }
        }

    override suspend fun createTag(input: CreateTagInput): AppResult<Unit> {
        return writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            localDataSource.createTag(ownedInput)
        }
    }

    override suspend fun updateTag(input: UpdateTagInput): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            check(localDataSource.updateTag(ownedInput)) {
                "Tag not found: ${input.localId}"
            }
        }

    override suspend fun deleteTag(localId: String): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            check(localDataSource.deleteTag(localId, ownerId)) { "Tag not found: $localId" }
        }

    override suspend fun syncNow(): AppResult<Unit> = syncCoordinator.syncNow()

    override suspend fun login(username: String, password: String) =
        remoteDataSource.login(username, password).also { loginResult ->
            if (loginResult.success) {
                syncCoordinator.enqueue()
            }
        }

    override suspend fun enableDevMode() = remoteDataSource.enableDevMode()

    override suspend fun checkBackendDevMode() = remoteDataSource.checkBackendDevMode()

    override suspend fun uploadHar(fileName: String, bytes: ByteArray) =
        remoteDataSource.uploadHar(fileName, bytes)

    override suspend fun clearSession() = sessionManager.clear()
    override suspend fun getDiscoveryJobs(): AppResult<List<DiscoveryJob>> {
        return remoteDataSource.getDiscoveryJobs()
    }

    private suspend fun localWrite(write: suspend () -> Unit): AppResult<Unit> = try {
        write()
        syncCoordinator.enqueue()
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Error(error.message ?: "Local write failed", error)
    }

    private fun observeEventsForUser(userId: Int): Flow<List<Event>> {
        return localDataSource.observeEvents(userId.toString()).map { entities ->
            entities.map { entity -> entity.toModel() }
        }
    }

    private fun observeTagsForUser(userId: Int): Flow<List<Tag>> {
        return localDataSource.observeTags(userId.toString()).map { entities ->
            entities.map { entity -> entity.toModel() }
        }
    }

    private suspend fun writeForCurrentUser(write: suspend (ownerId: String) -> Unit): AppResult<Unit> {
        val credentials = sessionManager.getCredentials()
        val ownerId = credentials?.userId?.toString()
            ?: return AppResult.Error("Login is required")
        return localWrite { write(ownerId) }
    }

    private fun EventEntity.toModel() = Event(
        localId = localId,
        remoteId = remoteId,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        tagLocalId = tagLocalId,
        tagName = tagName,
        ownerId = ownerId,
        ownerName = ownerName,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun TagEntity.toModel() = Tag(
        localId = localId,
        remoteId = remoteId,
        name = name,
        color = color,
        ownerId = ownerId,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
