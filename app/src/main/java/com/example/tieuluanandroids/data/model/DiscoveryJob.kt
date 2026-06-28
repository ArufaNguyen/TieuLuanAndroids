package com.example.tieuluanandroids.data.model

data class DiscoveryJob(
    val id: String,
    val userId: Int?,

    val fileName: String?,
    val status: String,
    val errorMessage: String?,
    val createdAt: String,
    val completedAt: String?
)