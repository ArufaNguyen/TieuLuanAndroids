package com.example.tieuluanandroids

import android.app.Application
import androidx.room.Room
import com.example.tieuluanandroids.model.service.SmartCalendarRemoteDataSource
import com.example.tieuluanandroids.model.local.RoomLocalDataSource
import com.example.tieuluanandroids.model.local.MIGRATION_1_2
import com.example.tieuluanandroids.model.local.MIGRATION_2_3
import com.example.tieuluanandroids.model.local.SmartCalendarDatabase
import com.example.tieuluanandroids.model.service.RoomSessionManager
import com.example.tieuluanandroids.model.service.SmartCalendarData
import com.example.tieuluanandroids.model.sync.SyncManager

class SmartCalendarApplication : Application() {
    // Ãƒâ€žÃ‚ÂÃƒÆ’Ã‚Â¢y lÃƒÆ’Ã‚Â  composition root: nÃƒâ€ Ã‚Â¡i duy nhÃƒÂ¡Ã‚ÂºÃ‚Â¥t lÃƒÂ¡Ã‚ÂºÃ‚Â¯p ghÃƒÆ’Ã‚Â©p cÃƒÆ’Ã‚Â¡c dependency cÃƒÂ¡Ã‚Â»Ã‚Â§a tÃƒÂ¡Ã‚ÂºÃ‚Â§ng data.
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
    val sessionManager: RoomSessionManager by lazy { RoomSessionManager(database.sessionDao()) }
    private val remoteDataSource by lazy { SmartCalendarRemoteDataSource(sessionManager) }

    val syncManager: SyncManager by lazy {
        SyncManager(applicationContext, localDataSource, remoteDataSource, sessionManager)
    }

    val data: SmartCalendarData by lazy {
        SmartCalendarData(localDataSource, remoteDataSource, syncManager, sessionManager)
    }

    override fun onCreate() {
        super.onCreate()
        syncManager.enqueue()
    }
}
