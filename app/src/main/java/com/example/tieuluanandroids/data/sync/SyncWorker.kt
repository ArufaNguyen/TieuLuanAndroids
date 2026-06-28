package com.example.tieuluanandroids.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tieuluanandroids.SmartCalendarApplication
import com.example.tieuluanandroids.data.model.AppResult

class SyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? SmartCalendarApplication
            ?: return Result.failure()
        if (application.sessionManager.getCredentials() == null) return Result.success()
        return when (application.syncManager.syncNow()) {
            is AppResult.Success -> Result.success()
            is AppResult.Error -> Result.retry()
        }
    }
}
