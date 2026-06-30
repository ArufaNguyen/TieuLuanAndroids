package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.sync.*

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SessionCredentials(
    val token: String,
    val userId: Int
)

class RoomSessionManager(
    private val sessionDao: SessionDao
) {
    fun observeSession(): Flow<SessionInfo?> {
        return sessionDao.observe().map { entity ->
            entity?.toSessionInfo()
        }
    }

    suspend fun getCredentials(): SessionCredentials? {
        val storedSession = sessionDao.get() ?: return null
        return SessionCredentials(
            token = storedSession.sessionToken,
            userId = storedSession.userId
        )
    }

    suspend fun save(token: String, session: SessionInfo) {
        require(token.isNotBlank()) { "Session token must not be blank" }
        sessionDao.save(session.toEntity(token))
    }

    suspend fun clear() {
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
