package com.example.tieuluanandroids.ui.login

import com.example.tieuluanandroids.data.api.ApiResult
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.Event
import com.example.tieuluanandroids.data.model.Tag
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import com.example.tieuluanandroids.data.model.SessionInfo
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful login emits message and navigation`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(FakeRepository())
        val events = async { viewModel.events.take(2).toList() }

        viewModel.login("user", "password")
        advanceUntilIdle()

        assertEquals(
            listOf(LoginEvent.ShowMessage("Login success"), LoginEvent.NavigateToEvents),
            events.await()
        )
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }
}

private class FakeRepository : SmartCalendarRepository {
    override val isDevMode = false

    override suspend fun login(username: String, password: String) =
        ApiResult(success = true, message = "Login success")

    override suspend fun enableDevMode() = Unit
    override suspend fun checkBackendDevMode() = ApiResult(true, "Dev mode")
    override suspend fun uploadHar(fileName: String, bytes: ByteArray) = ApiResult(true, "OK")
    override fun observeEvents(): Flow<List<Event>> = flowOf(emptyList())
    override fun observeTags(): Flow<List<Tag>> = flowOf(emptyList())
    override fun observeSession(): Flow<SessionInfo?> = flowOf(null)
    override suspend fun createEvent(input: CreateEventInput) = AppResult.Success(Unit)
    override suspend fun updateEvent(input: UpdateEventInput) = AppResult.Success(Unit)
    override suspend fun deleteEvent(localId: String) = AppResult.Success(Unit)
    override suspend fun createTag(input: CreateTagInput) = AppResult.Success(Unit)
    override suspend fun updateTag(input: UpdateTagInput) = AppResult.Success(Unit)
    override suspend fun deleteTag(localId: String) = AppResult.Success(Unit)
    override suspend fun syncNow() = AppResult.Success(Unit)
    override suspend fun clearSession() = Unit
}
