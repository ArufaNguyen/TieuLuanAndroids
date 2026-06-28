package com.example.tieuluanandroids.data.sync

import com.example.tieuluanandroids.data.model.AppResult

interface SyncCoordinator {
    fun enqueue()
    suspend fun syncNow(): AppResult<Unit>
}
