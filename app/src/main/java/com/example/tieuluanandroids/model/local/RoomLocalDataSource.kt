package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.UUID

class RoomLocalDataSource(
    private val database: SmartCalendarDatabase
) {
    private val events = database.eventDao()
    private val tags = database.tagDao()
    private val outbox = database.syncOutboxDao()

    fun observeEvents(ownerId: String): Flow<List<EventEntity>> = events.observeActive(ownerId)
    fun observeTags(ownerId: String): Flow<List<TagEntity>> = tags.observeActive(ownerId)

    // Event vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  bÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£n ghi chÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â sync luÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â´n ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¹Ãƒâ€¦Ã¢â‚¬Å“ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â£c cÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­p nhÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â­t trong cÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¹ng mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢t transaction.
    suspend fun createEvent(input: CreateEventInput) = database.withTransaction {
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        val tag = input.tagLocalId?.let { tags.getByLocalId(it) }
        events.upsert(
            EventEntity(
                localId = localId,
                remoteId = null,
                title = input.title,
                description = input.description,
                startTime = input.startTime,
                endTime = input.endTime,
                tagLocalId = input.tagLocalId,
                tagName = tag?.name ?: "-",
                ownerId = input.ownerId,
                ownerName = "-",
                syncStatus = SyncStatus.PENDING_CREATE,
                updatedAt = now,
                deletedAt = null
            )
        )
        outbox.upsert(
            createOutboxChange(
                entityType = SyncPolicy.ENTITY_EVENT,
                localId = localId,
                operation = SyncPolicy.OPERATION_CREATE,
                ownerId = input.ownerId,
                payload = createEventPayload(input, remoteId = null, tagRemoteId = tag?.remoteId),
                now = now
            )
        )
    }

    suspend fun updateEvent(input: UpdateEventInput): Boolean = database.withTransaction {
        val ownerId = input.ownerId ?: return@withTransaction false
        val current = events.getByLocalIdForOwner(input.localId, ownerId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        val tag = input.tagLocalId?.let { tags.getByLocalId(it) }
        val existingChange = outbox.findForEntity(SyncPolicy.ENTITY_EVENT, input.localId)
        val operation = if (current.remoteId == null) {
            SyncPolicy.OPERATION_CREATE
        } else {
            SyncPolicy.OPERATION_UPDATE
        }
        events.upsert(
            current.copy(
                title = input.title,
                description = input.description,
                startTime = input.startTime,
                endTime = input.endTime,
                tagLocalId = input.tagLocalId,
                tagName = tag?.name ?: "-",
                ownerId = input.ownerId,
                syncStatus = if (current.remoteId == null) {
                    SyncStatus.PENDING_CREATE
                } else {
                    SyncStatus.PENDING_UPDATE
                },
                updatedAt = now,
                deletedAt = null
            )
        )
        outbox.upsert(
            createOutboxChange(
                entityType = SyncPolicy.ENTITY_EVENT,
                localId = input.localId,
                operation = operation,
                ownerId = input.ownerId,
                payload = createEventPayload(input, current.remoteId, tag?.remoteId),
                now = existingChange?.createdAt ?: now,
                id = existingChange?.id
            )
        )
        true
    }

    suspend fun deleteEvent(localId: String, ownerId: String): Boolean = database.withTransaction {
        val current = events.getByLocalIdForOwner(localId, ownerId) ?: return@withTransaction false
        if (current.remoteId == null) {
            outbox.deleteForEntity(SyncPolicy.ENTITY_EVENT, localId)
            events.hardDelete(localId)
            return@withTransaction true
        }
        val now = System.currentTimeMillis()
        val existingChange = outbox.findForEntity(SyncPolicy.ENTITY_EVENT, localId)
        events.upsert(
            current.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = now,
                deletedAt = now
            )
        )
        outbox.upsert(
            createOutboxChange(
                SyncPolicy.ENTITY_EVENT,
                localId,
                SyncPolicy.OPERATION_DELETE,
                current.ownerId,
                JSONObject().put("remoteId", current.remoteId).toString(),
                existingChange?.createdAt ?: now,
                existingChange?.id
            )
        )
        true
    }

    // Tag cÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ng tuÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢n theo quy tÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚ÂºÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¯c ghi local trÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â°ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âºc giÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¹Ãƒâ€¦Ã¢â‚¬Å“ng Event.
    suspend fun createTag(input: CreateTagInput) = database.withTransaction {
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        tags.upsert(
            TagEntity(
                localId = localId,
                remoteId = null,
                name = input.name,
                color = input.color,
                ownerId = input.ownerId,
                syncStatus = SyncStatus.PENDING_CREATE,
                updatedAt = now,
                deletedAt = null
            )
        )
        outbox.upsert(
            createOutboxChange(
                SyncPolicy.ENTITY_TAG,
                localId,
                SyncPolicy.OPERATION_CREATE,
                input.ownerId,
                createTagPayload(input, null),
                now
            )
        )
    }

    suspend fun updateTag(input: UpdateTagInput): Boolean = database.withTransaction {
        val ownerId = input.ownerId ?: return@withTransaction false
        val current = tags.getByLocalIdForOwner(input.localId, ownerId) ?: return@withTransaction false
        val now = System.currentTimeMillis()
        val existingChange = outbox.findForEntity(SyncPolicy.ENTITY_TAG, input.localId)
        val operation = if (current.remoteId == null) {
            SyncPolicy.OPERATION_CREATE
        } else {
            SyncPolicy.OPERATION_UPDATE
        }
        tags.upsert(
            current.copy(
                name = input.name,
                color = input.color,
                ownerId = input.ownerId,
                syncStatus = if (current.remoteId == null) {
                    SyncStatus.PENDING_CREATE
                } else {
                    SyncStatus.PENDING_UPDATE
                },
                updatedAt = now,
                deletedAt = null
            )
        )
        outbox.upsert(
            createOutboxChange(
                SyncPolicy.ENTITY_TAG,
                input.localId,
                operation,
                input.ownerId,
                createTagPayload(input, current.remoteId),
                existingChange?.createdAt ?: now,
                existingChange?.id
            )
        )
        true
    }

    suspend fun deleteTag(localId: String, ownerId: String): Boolean = database.withTransaction {
        val current = tags.getByLocalIdForOwner(localId, ownerId) ?: return@withTransaction false
        if (current.remoteId == null) {
            outbox.deleteForEntity(SyncPolicy.ENTITY_TAG, localId)
            tags.hardDelete(localId)
            return@withTransaction true
        }
        val now = System.currentTimeMillis()
        val existingChange = outbox.findForEntity(SyncPolicy.ENTITY_TAG, localId)
        tags.upsert(current.copy(syncStatus = SyncStatus.PENDING_DELETE, updatedAt = now, deletedAt = now))
        outbox.upsert(
            createOutboxChange(
                SyncPolicy.ENTITY_TAG,
                localId,
                SyncPolicy.OPERATION_DELETE,
                current.ownerId,
                JSONObject().put("remoteId", current.remoteId).toString(),
                existingChange?.createdAt ?: now,
                existingChange?.id
            )
        )
        true
    }

    suspend fun pendingChanges(ownerId: String): List<SyncOutboxEntity> =
        outbox.getPending(ownerId)

    suspend fun markSyncSuccess(change: SyncOutboxEntity, remoteId: String?) =
        database.withTransaction {
            val now = System.currentTimeMillis()
            when (change.entityType) {
                SyncPolicy.ENTITY_EVENT -> {
                    val entity = events.getByLocalId(change.entityLocalId)
                    if (change.operation == SyncPolicy.OPERATION_DELETE) {
                        events.hardDelete(change.entityLocalId)
                    } else if (entity != null) {
                        events.updateSyncState(
                            entity.localId,
                            remoteId ?: entity.remoteId,
                            SyncStatus.SYNCED,
                            now,
                            null
                        )
                    }
                }
                SyncPolicy.ENTITY_TAG -> {
                    val entity = tags.getByLocalId(change.entityLocalId)
                    if (change.operation == SyncPolicy.OPERATION_DELETE) {
                        tags.hardDelete(change.entityLocalId)
                    } else if (entity != null) {
                        tags.updateSyncState(
                            entity.localId,
                            remoteId ?: entity.remoteId,
                            SyncStatus.SYNCED,
                            now,
                            null
                        )
                        val resolvedRemoteId = remoteId ?: entity.remoteId
                        if (resolvedRemoteId != null) {
                            outbox.getPending(entity.ownerId.orEmpty())
                                .filter { it.entityType == SyncPolicy.ENTITY_EVENT }
                                .forEach { pendingEvent ->
                                    val payload = JSONObject(pendingEvent.payloadJson)
                                    if (payload.optString("tagLocalId") == entity.localId) {
                                        outbox.upsert(
                                            pendingEvent.copy(
                                                payloadJson = payload
                                                    .put("tagId", resolvedRemoteId)
                                                    .toString()
                                            )
                                        )
                                    }
                                }
                        }
                    }
                }
            }
            outbox.delete(change.id)
        }

    suspend fun markSyncFailure(change: SyncOutboxEntity, error: String) =
        database.withTransaction {
            val now = System.currentTimeMillis()
            when (change.entityType) {
                SyncPolicy.ENTITY_EVENT -> events.getByLocalId(change.entityLocalId)?.let {
                    events.updateSyncState(it.localId, it.remoteId, SyncStatus.SYNC_FAILED, now, it.deletedAt)
                }
                SyncPolicy.ENTITY_TAG -> tags.getByLocalId(change.entityLocalId)?.let {
                    tags.updateSyncState(it.localId, it.remoteId, SyncStatus.SYNC_FAILED, now, it.deletedAt)
                }
            }
            outbox.markFailed(change.id, error)
        }

    suspend fun mergeRemoteTags(remoteTags: List<RemoteTag>) = database.withTransaction {
        remoteTags.forEach { remote ->
            val existing = tags.getByRemoteId(remote.id)
            if (existing != null && existing.syncStatus != SyncStatus.SYNCED) return@forEach
            tags.upsert(
                TagEntity(
                    localId = existing?.localId ?: "remote-tag-${remote.id}",
                    remoteId = remote.id,
                    name = remote.name,
                    color = remote.color,
                    ownerId = remote.ownerId,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null
                )
            )
        }
    }

    suspend fun mergeRemoteEvents(remoteEvents: List<RemoteEvent>) = database.withTransaction {
        remoteEvents.forEach { remote ->
            val existing = events.getByRemoteId(remote.id)
            if (existing != null && existing.syncStatus != SyncStatus.SYNCED) return@forEach
            val tag = remote.tagId?.let { tags.getByRemoteId(it) }
            events.upsert(
                EventEntity(
                    localId = existing?.localId ?: "remote-event-${remote.id}",
                    remoteId = remote.id,
                    title = remote.title,
                    description = remote.description,
                    startTime = remote.startTime,
                    endTime = remote.endTime,
                    tagLocalId = tag?.localId,
                    tagName = remote.tagName,
                    ownerId = remote.ownerId,
                    ownerName = remote.ownerName,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null
                )
            )
        }
    }

    private fun createOutboxChange(
        entityType: String,
        localId: String,
        operation: String,
        ownerId: String?,
        payload: String,
        now: Long,
        id: String? = null
    ) = SyncOutboxEntity(
        id = id ?: UUID.randomUUID().toString(),
        entityType = entityType,
        entityLocalId = localId,
        ownerId = ownerId,
        operation = operation,
        payloadJson = payload,
        createdAt = now
    )

    private fun createEventPayload(input: CreateEventInput, remoteId: String?, tagRemoteId: String?) =
        JSONObject()
            .putNullable("remoteId", remoteId)
            .put("title", input.title)
            .putNullable("description", input.description)
            .put("startTime", input.startTime)
            .put("endTime", input.endTime)
            .putNullable("tagId", tagRemoteId)
            .putNullable("tagLocalId", input.tagLocalId)
            .putNullable("userId", input.ownerId)
            .toString()

    private fun createEventPayload(input: UpdateEventInput, remoteId: String?, tagRemoteId: String?) =
        JSONObject()
            .putNullable("remoteId", remoteId)
            .put("title", input.title)
            .putNullable("description", input.description)
            .put("startTime", input.startTime)
            .put("endTime", input.endTime)
            .putNullable("tagId", tagRemoteId)
            .putNullable("tagLocalId", input.tagLocalId)
            .putNullable("userId", input.ownerId)
            .toString()

    private fun createTagPayload(input: CreateTagInput, remoteId: String?) = JSONObject()
        .putNullable("remoteId", remoteId)
        .put("name", input.name)
        .putNullable("color", input.color)
        .putNullable("userId", input.ownerId)
        .toString()

    private fun createTagPayload(input: UpdateTagInput, remoteId: String?) = JSONObject()
        .putNullable("remoteId", remoteId)
        .put("name", input.name)
        .putNullable("color", input.color)
        .putNullable("userId", input.ownerId)
        .toString()

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject =
        put(name, value ?: JSONObject.NULL)
}
