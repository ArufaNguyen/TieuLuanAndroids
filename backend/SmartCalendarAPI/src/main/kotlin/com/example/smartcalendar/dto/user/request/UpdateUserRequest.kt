package com.example.smartcalendar.dto.user.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UpdateUserRequest(
    @field:NotBlank(message = "username must not be blank")
    val username: String,

    @field:NotBlank(message = "email must not be blank")
    @field:Email(message = "email must be valid")
    val email: String,

    val fullName: String?
)
