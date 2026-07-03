package com.example.smartcalendar.dto.auth.request

import jakarta.validation.constraints.NotBlank

data class LogoutRequest(
    @field:NotBlank(message = "sessionToken must not be blank")
    val sessionToken: String
)
