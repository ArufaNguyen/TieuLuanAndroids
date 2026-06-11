package com.example.smartcalendar.dto.event.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateEventRequest(
    @field:NotBlank(message = "title must not be blank")
    val title: String,
    val description: String?,
    @field:NotNull(message = "startTime is required")
    val startTime: LocalDateTime?,
    @field:NotNull(message = "endTime is required")
    val endTime: LocalDateTime?,
    val tagId: Int?,
    val userId: Int?
)
