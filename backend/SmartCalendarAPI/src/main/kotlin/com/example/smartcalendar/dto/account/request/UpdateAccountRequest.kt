package com.example.smartcalendar.dto.account.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateAccountRequest(
    @field:NotBlank(message = "username must not be blank")
    val username: String,
    val loginName: String?,
    val password: String?,
    @field:NotNull(message = "userId is required")
    val userId: Int?
)
