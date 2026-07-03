package com.example.smartcalendar.dto.event.request

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class UpdateEventRequest(
    @field:NotBlank(message = "title must not be blank")
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val tagId: Int?,
    val userId: Int?
)
