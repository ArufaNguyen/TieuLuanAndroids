package com.example.tieuluanandroids.model.service

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.sync.*

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SmartCalendarRemoteDataSource(
    private val sessionManager: RoomSessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val isDevMode: Boolean
        get() = SmartCalendarApiClient.devMode

    suspend fun login(username: String, password: String) = withContext(ioDispatcher) {
        val result = SmartCalendarApiClient.login(username, password)
        val token = result.token
        val session = result.session
        if (result.success && token != null && session != null) {
            sessionManager.save(token, session)
        }
        ApiResult(result.success, result.message)
    }

    suspend fun enableDevMode() = withContext(ioDispatcher) {
        SmartCalendarApiClient.enableDevMode()
    }

    suspend fun checkBackendDevMode() = withContext(ioDispatcher) {
        val credentials = sessionManager.getCredentials()
            ?: return@withContext ApiResult(false, "Login is required")
        SmartCalendarApiClient.checkBackendDevMode(credentials.token)
    }

    suspend fun uploadHar(fileName: String, bytes: ByteArray) = withContext(ioDispatcher) {
        val credentials = sessionManager.getCredentials()
            ?: return@withContext ApiResult(false, "Login is required")
        SmartCalendarApiClient.uploadHar(credentials.token, credentials.userId, fileName, bytes)
    }

    suspend fun pullEvents() = withContext(ioDispatcher) {
        val credentials = sessionManager.getCredentials()
            ?: return@withContext RemoteListResult(false, "Login is required", emptyList())
        SmartCalendarApiClient.getRemoteEvents(credentials.token)
    }

    suspend fun pullTags() = withContext(ioDispatcher) {
        val credentials = sessionManager.getCredentials()
            ?: return@withContext RemoteListResult(false, "Login is required", emptyList())
        SmartCalendarApiClient.getRemoteTags(credentials.token)
    }

    suspend fun push(change: SyncOutboxEntity): RemoteWriteResult = withContext(ioDispatcher) {
        val token = sessionManager.getCredentials()?.token
            ?: return@withContext RemoteWriteResult(false, "Login is required")
        val storedPayload = JSONObject(change.payloadJson)

        if (hasUnresolvedTag(change, storedPayload)) {
            return@withContext RemoteWriteResult(false, "Referenced tag is still pending sync")
        }

        when (change.entityType) {
            SyncPolicy.ENTITY_EVENT -> pushEventChange(token, change, storedPayload)
            SyncPolicy.ENTITY_TAG -> pushTagChange(token, change, storedPayload)
            else -> return@withContext RemoteWriteResult(false, "Unknown entity type: ${change.entityType}")
        }
    }

    suspend fun getDiscoveryJobs(): AppResult<List<DiscoveryJob>> =
        withContext(ioDispatcher) {
            val credentials = sessionManager.getCredentials()
                ?: return@withContext AppResult.Error(
                    "Login is required"
                )

            SmartCalendarApiClient.getDiscoveryJobs(
                credentials.token
            )
        }

    suspend fun savePortalAuthorizationCredential(portalToken: String): ApiResult =
        withContext(ioDispatcher) {
            val credentials = sessionManager.getCredentials()
                ?: return@withContext ApiResult(false, "Login is required")
            SmartCalendarApiClient.savePortalAuthorizationCredential(
                credentials.token,
                portalToken
            )
        }

    suspend fun agentChatV2(message: String, confirmed: Boolean = false): AgentChatResult =
        withContext(ioDispatcher) {
            val credentials = sessionManager.getCredentials()
                ?: return@withContext AgentChatResult(false, "Login is required")
            SmartCalendarApiClient.agentChatV2(credentials.token, message, confirmed)
        }



    private fun pushEventChange(
        token: String,
        change: SyncOutboxEntity,
        storedPayload: JSONObject
    ): RemoteWriteResult {
        val apiPayload = buildEventApiPayload(storedPayload)
        return when (change.operation) {
            SyncPolicy.OPERATION_CREATE -> SmartCalendarApiClient.createRemoteEvent(token, apiPayload)
            SyncPolicy.OPERATION_UPDATE -> withRemoteId(change, storedPayload) { remoteId ->
                SmartCalendarApiClient.updateRemoteEvent(token, remoteId, apiPayload)
            }
            SyncPolicy.OPERATION_DELETE -> withRemoteId(change, storedPayload) { remoteId ->
                SmartCalendarApiClient.deleteRemoteEvent(token, remoteId)
            }
            else -> RemoteWriteResult(false, "Unknown event operation: ${change.operation}")
        }
    }

    private fun pushTagChange(
        token: String,
        change: SyncOutboxEntity,
        storedPayload: JSONObject
    ): RemoteWriteResult {
        val apiPayload = buildTagApiPayload(storedPayload)
        return when (change.operation) {
            SyncPolicy.OPERATION_CREATE -> SmartCalendarApiClient.createRemoteTag(token, apiPayload)
            SyncPolicy.OPERATION_UPDATE -> withRemoteId(change, storedPayload) { remoteId ->
                SmartCalendarApiClient.updateRemoteTag(token, remoteId, apiPayload)
            }
            SyncPolicy.OPERATION_DELETE -> withRemoteId(change, storedPayload) { remoteId ->
                SmartCalendarApiClient.deleteRemoteTag(token, remoteId)
            }
            else -> RemoteWriteResult(false, "Unknown tag operation: ${change.operation}")
        }
    }

    private fun hasUnresolvedTag(change: SyncOutboxEntity, payload: JSONObject): Boolean {
        val isEventWrite = change.entityType == SyncPolicy.ENTITY_EVENT &&
            change.operation != SyncPolicy.OPERATION_DELETE
        val referencesLocalTag = payload.nullableString("tagLocalId") != null
        val hasRemoteTagId = payload.nullableString("tagId") != null
        return isEventWrite && referencesLocalTag && !hasRemoteTagId
    }

    private fun buildEventApiPayload(stored: JSONObject) = JSONObject()
        .put("title", stored.optString("title"))
        .putNullable("description", stored.nullableString("description"))
        .put("startTime", stored.optString("startTime"))
        .put("endTime", stored.optString("endTime"))
        .putNullable("tagId", stored.nullableInt("tagId"))
        .putNullable("userId", stored.nullableInt("userId"))

    private fun buildTagApiPayload(stored: JSONObject) = JSONObject()
        .put("name", stored.optString("name"))
        .putNullable("color", stored.nullableString("color"))
        .putNullable("userId", stored.nullableInt("userId"))

    private fun JSONObject.nullableString(name: String): String? =
        takeUnless { isNull(name) }?.optString(name)?.takeIf { it.isNotBlank() }

    private fun JSONObject.nullableInt(name: String): Int? = nullableString(name)?.toIntOrNull()

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
        put(name, value ?: JSONObject.NULL)

    private inline fun withRemoteId(
        change: SyncOutboxEntity,
        payload: JSONObject,
        action: (String) -> RemoteWriteResult
    ): RemoteWriteResult {
        val remoteId = payload.nullableString("remoteId")
        return if (remoteId == null) {
            RemoteWriteResult(false, "Missing remoteId for ${change.entityType} ${change.operation}")
        } else {
            action(remoteId)
        }
    }
}
