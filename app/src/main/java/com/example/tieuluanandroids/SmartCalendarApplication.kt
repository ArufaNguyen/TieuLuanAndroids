package com.example.tieuluanandroids

import android.app.Application
import androidx.room.Room
import com.example.tieuluanandroids.data.api.SmartCalendarRemoteDataSource
import com.example.tieuluanandroids.data.local.RoomLocalDataSource
import com.example.tieuluanandroids.data.local.MIGRATION_1_2
import com.example.tieuluanandroids.data.local.MIGRATION_2_3
import com.example.tieuluanandroids.data.local.SmartCalendarDatabase
import com.example.tieuluanandroids.data.repository.DefaultSmartCalendarRepository
import com.example.tieuluanandroids.data.repository.SmartCalendarRepository
import com.example.tieuluanandroids.data.sync.SyncManager
import com.example.tieuluanandroids.data.session.RoomSessionManager
import com.example.tieuluanandroids.data.session.SessionManager

class SmartCalendarApplication : Application() {
    // Đây là composition root: nơi duy nhất lắp ghép các dependency của tầng data.
    private val database: SmartCalendarDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SmartCalendarDatabase::class.java,
            "smart-calendar.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    private val localDataSource by lazy { RoomLocalDataSource(database) }
    val sessionManager: SessionManager by lazy { RoomSessionManager(database.sessionDao()) }
    private val remoteDataSource by lazy { SmartCalendarRemoteDataSource(sessionManager) }

    val syncManager: SyncManager by lazy {
        SyncManager(applicationContext, localDataSource, remoteDataSource, sessionManager)
    }

    val repository: SmartCalendarRepository by lazy {
        DefaultSmartCalendarRepository(localDataSource, remoteDataSource, syncManager, sessionManager)
    }

    override fun onCreate() {
        super.onCreate()
        syncManager.enqueue()
    }
}
