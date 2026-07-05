package com.example.tieuluanandroids.model.local

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.service.*
import com.example.tieuluanandroids.model.sync.*

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `active_session` (
                `id` INTEGER NOT NULL,
                `sessionToken` TEXT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `userId` INTEGER NOT NULL,
                `username` TEXT NOT NULL,
                `loginName` TEXT,
                `email` TEXT NOT NULL,
                `fullName` TEXT,
                `expiresAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sync_outbox` ADD COLUMN `ownerId` TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_outbox_ownerId` " +
                "ON `sync_outbox` (`ownerId`)"
        )
    }
}
