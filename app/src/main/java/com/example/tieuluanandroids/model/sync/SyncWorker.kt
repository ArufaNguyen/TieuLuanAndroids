package com.example.tieuluanandroids.model.sync

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.service.*

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tieuluanandroids.SmartCalendarApplication

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
