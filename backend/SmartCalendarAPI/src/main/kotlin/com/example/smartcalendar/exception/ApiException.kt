package com.example.smartcalendar.exception

class ApiException(
    val code: Int,
    override val message: String
) : RuntimeException(message)
