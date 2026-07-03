package com.example.smartcalendar.repository

import com.example.smartcalendar.model.PortalCredential
import org.springframework.data.jpa.repository.JpaRepository

interface PortalCredentialRepository : JpaRepository<PortalCredential, Int> {
    fun findByCaptureId(captureId: String): PortalCredential?
    fun findFirstByUserIdOrderByLastCapturedAtDescIdDesc(userId: Int): PortalCredential?
}

