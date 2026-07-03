package com.example.smartcalendar.service

import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.User
import org.springframework.stereotype.Service

@Service
class SessionAuthorizationService(
    private val authService: AuthService
) {
    fun requireUser(sessionToken: String?, requestedUserId: Int? = null): User {
        val session = authService.getValidSession(sessionToken)
            ?: throw ApiException(401, "valid session is required")
        val user = session.account?.user
            ?: throw ApiException(401, "valid session is required")
        if (requestedUserId != null && requestedUserId != user.id) {
            throw ApiException(403, "userId does not match the active session")
        }
        return user
    }
}
