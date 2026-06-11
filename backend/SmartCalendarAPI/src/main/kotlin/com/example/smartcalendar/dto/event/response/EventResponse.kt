package com.example.smartcalendar.dto.event.response

import java.time.LocalDateTime

data class EventResponse(
    val id: Int,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val tagId: Int?,
    val tagName: String?,
    val userId: Int?
)
