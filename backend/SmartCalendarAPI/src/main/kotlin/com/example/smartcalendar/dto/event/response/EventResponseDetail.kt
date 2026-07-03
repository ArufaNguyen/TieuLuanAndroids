package com.example.smartcalendar.dto.event.response

import com.example.smartcalendar.dto.tag.response.TagResponse
import com.example.smartcalendar.dto.user.response.UserResponse
import java.time.LocalDateTime

data class EventResponseDetail(
    val id: Int,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val tag: TagResponse?,
    val user: UserResponse?
)
