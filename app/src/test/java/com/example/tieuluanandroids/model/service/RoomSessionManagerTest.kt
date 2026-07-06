package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.SessionInfo
import com.example.tieuluanandroids.model.local.SessionDao
import com.example.tieuluanandroids.model.local.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomSessionManagerTest {
    @Test
    fun `save exposes session and credentials then clear removes them`() = runTest {
        val manager = RoomSessionManager(InMemorySessionDao())
        val session = SessionInfo(
            accountId = 10,
            userId = 20,
            username = "tester",
            loginName = "tester-login",
            email = "tester@example.com",
            fullName = "Test User",
            expiresAt = "2026-07-21T10:00:00"
        )

        manager.save("plain-session-token", session)

        assertEquals(session, manager.observeSession().first())
        assertEquals(
            SessionCredentials(
                token = "plain-session-token",
                accountId = 10,
                userId = 20,
                username = "tester",
                loginName = "tester-login"
            ),
            manager.getCredentials()
        )

        manager.clear()
        assertNull(manager.getCredentials())
        assertNull(manager.observeSession().first())
    }
}

private class InMemorySessionDao : SessionDao {
    private val session = MutableStateFlow<SessionEntity?>(null)

    override fun observe(): Flow<SessionEntity?> = session
    override suspend fun get(): SessionEntity? = session.value
    override suspend fun save(session: SessionEntity) {
        this.session.value = session
    }
    override suspend fun clear() {
        session.value = null
    }
}
