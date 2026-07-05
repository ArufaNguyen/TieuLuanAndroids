package com.example.smartcalendar.repository

import com.example.smartcalendar.model.DiscoveryJob
import org.springframework.data.jpa.repository.JpaRepository

interface DiscoveryJobRepository : JpaRepository<DiscoveryJob, String> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Int): List<DiscoveryJob>
}
