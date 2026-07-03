package com.example.smartcalendar.dto.account.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateAccountRequest(
    @field:NotBlank(message = "username must not be blank")
    val username: String,
    val loginName: String?,
    @field:NotBlank(message = "password must not be blank")
    val password: String,
    @field:NotNull(message = "userId is required")
    val userId: Int?
)
