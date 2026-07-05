package com.example.smartcalendar.dto.tag.response

import com.example.smartcalendar.dto.user.response.UserResponse

data class TagResponseDetail(
    val id: Int,
    val name: String,
    val color: String?,
    val user: UserResponse?
)
