package com.example.smartcalendar.service

import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.Account
import com.example.smartcalendar.model.Session
import com.example.smartcalendar.model.User
import com.example.smartcalendar.repository.DiscoveryJobRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.unit.DataSize

class ReverseApiDiscoveryServiceTest {
    private val authService = mock<AuthService>()
    private val service = ReverseApiDiscoveryService(
        jobRepository = mock<DiscoveryJobRepository>(),
        authService = authService,
        processor = mock<ReverseApiDiscoveryProcessor>(),
        json = jacksonObjectMapper(),
        reverseApiProperties = ReverseApiProperties(DataSize.ofMegabytes(30))
    )

    @Test
    fun `rejects files larger than configured maximum`() {
        val file = MockMultipartFile("file", "portal.har", "application/json", ByteArray(30 * 1024 * 1024 + 1))

        val exception = assertThrows(ApiException::class.java) { service.create(file, "token", 1) }

        assertEquals(413, exception.code)
    }

    @Test
    fun `requires session for custom HAR upload`() {
        val file = MockMultipartFile("file", "portal.har", "application/json", validHar())

        val exception = assertThrows(ApiException::class.java) { service.create(file, null, 1) }

        assertEquals(401, exception.code)
    }

    @Test
    fun `rejects invalid HAR content`() {
        val file = MockMultipartFile("file", "portal.har", "application/json", "{}".toByteArray())
        whenever(authService.getValidSession("token")).thenReturn(session(1))

        val exception = assertThrows(ApiException::class.java) { service.create(file, "token", 1) }

        assertEquals(400, exception.code)
    }

    @Test
    fun `rejects user id that does not match session`() {
        val file = MockMultipartFile("file", "portal.har", "application/json", validHar())
        whenever(authService.getValidSession("token")).thenReturn(session(1))

        val exception = assertThrows(ApiException::class.java) { service.create(file, "token", 2) }

        assertEquals(403, exception.code)
    }

    private fun validHar() = """{"log":{"entries":[]}}""".toByteArray()

    private fun session(userId: Int) = Session(
        sessionToken = "token",
        account = Account(user = User(id = userId))
    )
}
