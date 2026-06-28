package com.example.tieuluanandroids.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tieuluanandroids.data.local.dao.EventDao
import com.example.tieuluanandroids.data.local.dao.SessionDao
import com.example.tieuluanandroids.data.local.dao.SyncOutboxDao
import com.example.tieuluanandroids.data.local.dao.TagDao
import com.example.tieuluanandroids.data.local.entity.EventEntity
import com.example.tieuluanandroids.data.local.entity.SessionEntity
import com.example.tieuluanandroids.data.local.entity.SyncOutboxEntity
import com.example.tieuluanandroids.data.local.entity.TagEntity

@Database(
    entities = [EventEntity::class, TagEntity::class, SyncOutboxEntity::class, SessionEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class SmartCalendarDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun tagDao(): TagDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun sessionDao(): SessionDao
}
