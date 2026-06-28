package com.example.tieuluanandroids.data.session

import com.example.tieuluanandroids.data.local.dao.SessionDao
import com.example.tieuluanandroids.data.local.entity.SessionEntity
import com.example.tieuluanandroids.data.model.SessionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SessionCredentials(
    val token: String,
    val userId: Int
)

interface SessionManager {
    fun observeSession(): Flow<SessionInfo?>
    suspend fun getCredentials(): SessionCredentials?
    suspend fun save(token: String, session: SessionInfo)
    suspend fun clear()
}

class RoomSessionManager(
    private val sessionDao: SessionDao
) : SessionManager {
    override fun observeSession(): Flow<SessionInfo?> {
        return sessionDao.observe().map { entity ->
            entity?.toSessionInfo()
        }
    }

    override suspend fun getCredentials(): SessionCredentials? {
        val storedSession = sessionDao.get() ?: return null
        return SessionCredentials(
            token = storedSession.sessionToken,
            userId = storedSession.userId
        )
    }

    override suspend fun save(token: String, session: SessionInfo) {
        require(token.isNotBlank()) { "Session token must not be blank" }
        sessionDao.save(session.toEntity(token))
    }

    override suspend fun clear() {
        sessionDao.clear()
    }
}

private fun SessionEntity.toSessionInfo() = SessionInfo(
    accountId = accountId,
    userId = userId,
    username = username,
    loginName = loginName,
    email = email,
    fullName = fullName,
    expiresAt = expiresAt
)

private fun SessionInfo.toEntity(token: String) = SessionEntity(
    sessionToken = token,
    accountId = accountId,
    userId = userId,
    username = username,
    loginName = loginName,
    email = email,
    fullName = fullName,
    expiresAt = expiresAt
)
