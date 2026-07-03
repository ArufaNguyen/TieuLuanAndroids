package com.example.smartcalendar.dto.auth.response

import java.time.LocalDateTime

data class AuthResponse(
    val sessionToken: String,
    val accountId: Int,
    val userId: Int,
    val username: String,
    val loginName: String?,
    val email: String,
    val fullName: String?,
    val expiresAt: LocalDateTime
)
