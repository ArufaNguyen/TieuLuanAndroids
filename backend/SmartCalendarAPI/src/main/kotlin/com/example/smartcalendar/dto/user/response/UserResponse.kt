package com.example.smartcalendar.dto.user.response

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val fullName: String?
)
