package com.example.smartcalendar.dto.auth.request

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "loginName must not be blank")
    val loginName: String,

    @field:NotBlank(message = "password must not be blank")
    val password: String
)
