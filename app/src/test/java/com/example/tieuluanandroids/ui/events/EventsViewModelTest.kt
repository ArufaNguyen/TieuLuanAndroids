package com.example.tieuluanandroids.ui.events

import com.example.tieuluanandroids.MainDispatcherRule
import com.example.tieuluanandroids.data.api.ApiResult
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.model.SyncStatus
import com.example.tieuluanandroids.data.model.Tag
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `cached Room data is exposed before network is required`() = runTest {
        val cached = sampleEvent()
        val repository = EventsFakeRepository(events = listOf(cached))

        val viewModel = EventsViewModel(repository)
        advanceUntilIdle()

        assertEquals(listOf(cached), viewModel.uiState.value.events)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sync error keeps cached data and emits one error`() = runTest {
        val cached = sampleEvent()
        val repository = EventsFakeRepository(
            events = listOf(cached),
            syncResult = AppResult.Error("Offline")
        )
        val viewModel = EventsViewModel(repository)
        val error = async { viewModel.errors.first() }

        advanceUntilIdle()

        assertEquals("Offline", error.await())
        assertEquals(listOf(cached), viewModel.uiState.value.events)
    }

    @Test
    fun `duplicate refresh is blocked while sync is running`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = EventsFakeRepository(syncGate = gate)
        val viewModel = EventsViewModel(repository)
        runCurrent()

        viewModel.refresh()
        viewModel.refresh()
        runCurrent()

        assertEquals(1, repository.syncCalls)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    private fun sampleEvent() = Event(
        localId = "local-1",
        remoteId = "1",
        title = "Cached",
        description = null,
        startTime = "2026-01-01T08:00:00",
        endTime = "2026-01-01T09:00:00",
        tagLocalId = null,
        tagName = "-",
        ownerId = null,
        ownerName = "-",
        syncStatus = SyncStatus.SYNCED,
        updatedAt = 1L,
        deletedAt = null
    )
}

private class EventsFakeRepository(
    events: List<Event> = emptyList(),
    private val syncResult: AppResult<Unit> = AppResult.Success(Unit),
    private val syncGate: CompletableDeferred<Unit>? = null
) : SmartCalendarRepository {
    private val eventFlow = MutableStateFlow(events)
    var syncCalls = 0
        private set

    override val isDevMode = false
    override fun observeEvents(): Flow<List<Event>> = eventFlow
    override fun observeTags(): Flow<List<Tag>> = MutableStateFlow(emptyList())
    override fun observeSession(): Flow<SessionInfo?> = MutableStateFlow(null)

    override suspend fun syncNow(): AppResult<Unit> {
        syncCalls++
        syncGate?.await()
        return syncResult
    }

    override suspend fun login(username: String, password: String) = ApiResult(true, "OK")
    override suspend fun enableDevMode() = Unit
    override suspend fun checkBackendDevMode() = ApiResult(true, "OK")
    override suspend fun uploadHar(fileName: String, bytes: ByteArray) = ApiResult(true, "OK")
    override suspend fun createEvent(input: CreateEventInput) = AppResult.Success(Unit)
    override suspend fun updateEvent(input: UpdateEventInput) = AppResult.Success(Unit)
    override suspend fun deleteEvent(localId: String) = AppResult.Success(Unit)
    override suspend fun createTag(input: CreateTagInput) = AppResult.Success(Unit)
    override suspend fun updateTag(input: UpdateTagInput) = AppResult.Success(Unit)
    override suspend fun deleteTag(localId: String) = AppResult.Success(Unit)
    override suspend fun clearSession() = Unit
}
