package com.example.smartcalendar.service

import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.Account
import com.example.smartcalendar.model.Session
import com.example.smartcalendar.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SessionAuthorizationServiceTest {
    private val authService = mock<AuthService>()
    private val authorization = SessionAuthorizationService(authService)

    @Test
    fun `missing or invalid session is rejected`() {
        whenever(authService.getValidSession(null)).thenReturn(null)

        val error = assertThrows<ApiException> {
            authorization.requireUser(null, 1)
        }

        assertEquals(401, error.code)
    }

    @Test
    fun `requested user id must match active session`() {
        whenever(authService.getValidSession("token")).thenReturn(session(userId = 1))

        val error = assertThrows<ApiException> {
            authorization.requireUser("token", requestedUserId = 2)
        }

        assertEquals(403, error.code)
    }

    @Test
    fun `matching session user is returned`() {
        val session = session(userId = 1)
        whenever(authService.getValidSession("token")).thenReturn(session)

        val user = authorization.requireUser("token", requestedUserId = 1)

        assertSame(session.account?.user, user)
    }

    private fun session(userId: Int) = Session(
        sessionToken = "token",
        account = Account(user = User(id = userId))
    )
}
