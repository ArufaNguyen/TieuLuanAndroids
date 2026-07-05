package com.example.smartcalendar.dto.tag.request

import jakarta.validation.constraints.NotBlank

data class CreateTagRequest(
    @field:NotBlank(message = "name must not be blank")
    val name: String,
    val color: String?,
    val userId: Int?
)
