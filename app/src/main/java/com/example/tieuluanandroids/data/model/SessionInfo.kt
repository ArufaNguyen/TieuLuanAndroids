package com.example.tieuluanandroids.data.model

data class SessionInfo(
    val accountId: Int,
    val userId: Int,
    val username: String,
    val loginName: String?,
    val email: String,
    val fullName: String?,
    val expiresAt: String
)
