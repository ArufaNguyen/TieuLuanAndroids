package com.example.tieuluanandroids.data.api

import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.model.DiscoveryJob

interface RemoteDataSource {
    val isDevMode: Boolean

    suspend fun login(username: String, password: String): ApiResult
    suspend fun enableDevMode()
    suspend fun checkBackendDevMode(): ApiResult
    suspend fun uploadHar(fileName: String, bytes: ByteArray): ApiResult
    suspend fun pullEvents(): RemoteListResult<RemoteEvent>
    suspend fun pullTags(): RemoteListResult<RemoteTag>
    suspend fun push(change: SyncOutboxEntity): RemoteWriteResult
    suspend fun getDiscoveryJobs(): AppResult<List<DiscoveryJob>>

}
