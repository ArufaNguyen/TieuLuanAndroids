package com.example.smartcalendar.dto.account.response

import com.example.smartcalendar.dto.user.response.UserResponse
import java.time.LocalDateTime

data class AccountResponseDetail(
    val id: Int,
    val username: String,
    val loginName: String?,
    val createdAt: LocalDateTime,
    val user: UserResponse?
)
