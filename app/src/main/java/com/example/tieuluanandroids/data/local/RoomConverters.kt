package com.example.tieuluanandroids.data.local

import androidx.room.TypeConverter
import com.example.tieuluanandroids.data.model.SyncStatus

class RoomConverters {
    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
