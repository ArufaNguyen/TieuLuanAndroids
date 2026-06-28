package com.example.tieuluanandroids.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_session")
data class SessionEntity(
    @PrimaryKey val id: Int = ACTIVE_SESSION_ID,
    val sessionToken: String,
    val accountId: Int,
    val userId: Int,
    val username: String,
    val loginName: String?,
    val email: String,
    val fullName: String?,
    val expiresAt: String
) {
    companion object {
        const val ACTIVE_SESSION_ID = 1
    }
}
