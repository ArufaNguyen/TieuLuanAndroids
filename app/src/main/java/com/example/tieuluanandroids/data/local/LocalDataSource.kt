package com.example.tieuluanandroids.data.local

import com.example.tieuluanandroids.data.api.RemoteEvent
import com.example.tieuluanandroids.data.api.RemoteTag
import com.example.tieuluanandroids.data.local.entity.EventEntity
import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity
import com.example.tieuluanandroids.data.local.entity.TagEntity
import com.example.tieuluanandroids.data.model.CreateEventInput
import com.example.tieuluanandroids.data.model.CreateTagInput
import com.example.tieuluanandroids.data.model.UpdateEventInput
import com.example.tieuluanandroids.data.model.UpdateTagInput
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {
    fun observeEvents(ownerId: String): Flow<List<EventEntity>>
    fun observeTags(ownerId: String): Flow<List<TagEntity>>

    suspend fun createEvent(input: CreateEventInput)
    suspend fun updateEvent(input: UpdateEventInput): Boolean
    suspend fun deleteEvent(localId: String, ownerId: String): Boolean
    suspend fun createTag(input: CreateTagInput)
    suspend fun updateTag(input: UpdateTagInput): Boolean
    suspend fun deleteTag(localId: String, ownerId: String): Boolean

    suspend fun pendingChanges(ownerId: String): List<SyncOutboxEntity>
    suspend fun markSyncSuccess(change: SyncOutboxEntity, remoteId: String?)
    suspend fun markSyncFailure(change: SyncOutboxEntity, error: String)
    suspend fun mergeRemoteEvents(remoteEvents: List<RemoteEvent>)
    suspend fun mergeRemoteTags(remoteTags: List<RemoteTag>)
}
