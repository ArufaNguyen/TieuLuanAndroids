package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
