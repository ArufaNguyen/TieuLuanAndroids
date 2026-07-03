package com.example.smartcalendar.dto.account.response

import java.time.LocalDateTime

data class AccountResponse(
    val id: Int,
    val username: String,
    val loginName: String?,
    val userId: Int,
    val createdAt: LocalDateTime
)
