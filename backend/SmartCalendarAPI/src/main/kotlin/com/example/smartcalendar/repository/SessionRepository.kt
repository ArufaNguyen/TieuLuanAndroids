package com.example.smartcalendar.repository

import com.example.smartcalendar.model.Session
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SessionRepository : JpaRepository<Session, Int> {

    fun findBySessionToken(sessionToken: String): Optional<Session>
}
