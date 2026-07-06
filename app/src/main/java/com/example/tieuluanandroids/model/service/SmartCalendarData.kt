package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.sync.*

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SmartCalendarData(
    private val localDataSource: RoomLocalDataSource,
    private val remoteDataSource: SmartCalendarRemoteDataSource,
    private val syncManager: SyncManager,
    private val sessionManager: RoomSessionManager
) {
    val isDevMode: Boolean
        get() = remoteDataSource.isDevMode

    fun observeEvents(): Flow<List<Event>> {
        return sessionManager.observeSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                observeEventsForUser(session.userId)
            }
        }
    }

    fun observeTags(): Flow<List<Tag>> {
        return sessionManager.observeSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                observeTagsForUser(session.userId)
            }
        }
    }

    fun observeSession(): Flow<SessionInfo?> = sessionManager.observeSession()

    suspend fun createEvent(input: CreateEventInput): AppResult<Unit> {
        return writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            localDataSource.createEvent(ownedInput)
        }
    }

    suspend fun updateEvent(input: UpdateEventInput): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            check(localDataSource.updateEvent(ownedInput)) {
                "Event not found: ${input.localId}"
            }
        }

    suspend fun deleteEvent(localId: String): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            check(localDataSource.deleteEvent(localId, ownerId)) { "Event not found: $localId" }
        }

    suspend fun createTag(input: CreateTagInput): AppResult<Unit> {
        return writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            localDataSource.createTag(ownedInput)
        }
    }

    suspend fun updateTag(input: UpdateTagInput): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            val ownedInput = input.copy(ownerId = ownerId)
            check(localDataSource.updateTag(ownedInput)) {
                "Tag not found: ${input.localId}"
            }
        }

    suspend fun deleteTag(localId: String): AppResult<Unit> =
        writeForCurrentUser { ownerId ->
            check(localDataSource.deleteTag(localId, ownerId)) { "Tag not found: $localId" }
        }

    suspend fun syncNow(): AppResult<Unit> = syncManager.syncNow()

    suspend fun login(username: String, password: String) =
        remoteDataSource.login(username, password).also { loginResult ->
            if (loginResult.success) {
                syncManager.enqueue()
            }
        }

    suspend fun register(username: String, password: String) =
        remoteDataSource.register(
            username = username,
            email = "${username.lowercase(Locale.US)}@smartcalendar.local",
            fullName = username,
            loginName = username,
            password = password
        ).also { registerResult ->
            if (registerResult.success) {
                syncManager.enqueue()
            }
        }

    suspend fun enableDevMode() = remoteDataSource.enableDevMode()

    suspend fun checkBackendDevMode() = remoteDataSource.checkBackendDevMode()

    suspend fun changePassword(oldPassword: String, newPassword: String) =
        remoteDataSource.changePassword(oldPassword, newPassword)

    suspend fun uploadHar(fileName: String, bytes: ByteArray) =
        remoteDataSource.uploadHar(fileName, bytes)

    suspend fun clearSession() = sessionManager.clear()

    suspend fun getDiscoveryJobs(): AppResult<List<DiscoveryJob>> {
        return remoteDataSource.getDiscoveryJobs()
    }

    suspend fun savePortalCredential(
        authorization: String?,
        cookie: String?,
        csrfToken: String?
    ) = remoteDataSource.savePortalCredential(authorization, cookie, csrfToken)

    suspend fun agentChatV2(message: String, confirmed: Boolean = false) =
        remoteDataSource.agentChatV2(message, confirmed)

    private suspend fun localWrite(write: suspend () -> Unit): AppResult<Unit> = try {
        write()
        syncManager.enqueue()
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
