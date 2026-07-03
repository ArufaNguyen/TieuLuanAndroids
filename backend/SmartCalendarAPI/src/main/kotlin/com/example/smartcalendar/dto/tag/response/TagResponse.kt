package com.example.smartcalendar.dto.tag.response

data class TagResponse(
    val id: Int,
    val name: String,
    val color: String?,
    val userId: Int?
)
