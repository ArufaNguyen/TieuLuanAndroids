package com.example.tieuluanandroids.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tieuluanandroids.data.api.RemoteDataSource
import com.example.tieuluanandroids.data.api.RemoteWriteResult
import com.example.tieuluanandroids.data.local.LocalDataSource
import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity
import com.example.tieuluanandroids.data.model.AppResult
import com.example.tieuluanandroids.data.session.SessionManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncManager(
    context: Context,
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val sessionManager: SessionManager
) : SyncCoordinator {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val syncMutex = Mutex()

    override fun enqueue() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            SyncPolicy.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun syncNow(): AppResult<Unit> = syncMutex.withLock {
        val credentials = sessionManager.getCredentials()
        val ownerId = credentials?.userId?.toString()
            ?: return@withLock AppResult.Error("Login is required")

        val pushError = pushPendingChanges(ownerId)
        val tagPullError = pullTagsIntoRoom()
        val eventPullError = pullEventsIntoRoom()
        val firstError = pushError ?: tagPullError ?: eventPullError

        if (firstError == null) {
            AppResult.Success(Unit)
        } else {
            AppResult.Error(firstError)
        }
    }

    private suspend fun pushPendingChanges(ownerId: String): String? {
        var firstError: String? = null
        val pendingChanges = localDataSource.pendingChanges(ownerId)

        for (change in pendingChanges) {
            val result = pushSafely(change)
            if (result.success) {
                localDataSource.markSyncSuccess(change, result.remoteId)
            } else {
                localDataSource.markSyncFailure(change, result.message)
                if (firstError == null) {
                    firstError = result.message
                }
            }
        }
        return firstError
    }

    private suspend fun pushSafely(change: SyncOutboxEntity): RemoteWriteResult {
        return try {
            remoteDataSource.push(change)
        } catch (error: Exception) {
            RemoteWriteResult(
                success = false,
                message = error.message ?: "Sync push failed"
            )
        }
    }

    private suspend fun pullTagsIntoRoom(): String? {
        val result = remoteDataSource.pullTags()
        if (!result.success) {
            return result.message
        }
        localDataSource.mergeRemoteTags(result.items)
        return null
    }

    private suspend fun pullEventsIntoRoom(): String? {
        val result = remoteDataSource.pullEvents()
        if (!result.success) {
            return result.message
        }
        localDataSource.mergeRemoteEvents(result.items)
        return null
    }
}
