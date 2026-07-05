package com.example.smartcalendar.repository

import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.model.DiscoveryJob
import com.example.smartcalendar.model.User
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.KnowledgeSource
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.portaldiscovery.knowledge.AuthType
import com.example.smartcalendar.portaldiscovery.knowledge.KnowledgeStatus
import com.example.smartcalendar.portaldiscovery.knowledge.SafetyLevel
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DatabaseApiKnowledgeRepositoryTest {
    private val jpaRepository = mock<ApiKnowledgeJpaRepository>()
    private val user = User(id = 1)
    private val job = DiscoveryJob(user = user)
    private val repository = DatabaseApiKnowledgeRepository(jpaRepository, jacksonObjectMapper(), user, job)

    @Test
    fun `custom user knowledge prefixes tool name with user id`() {
        whenever(jpaRepository.findByUserIdAndNaturalKey(any(), any())).thenReturn(null)
        whenever(jpaRepository.save(any())).thenAnswer { invocation ->
            invocation.getArgument<ApiKnowledgeEntity>(0).apply { id = 99 }
        }

        val saved = repository.saveAll(listOf(scheduleKnowledge())).single()

        assertEquals(99, saved.id)
        assertEquals("user_1_get_student_schedule", saved.toolName)
    }

    private fun scheduleKnowledge() = ApiKnowledge(
        toolName = "get_student_schedule",
        purpose = "Get the student's schedule.",
        category = EndpointCategory.SCHEDULE,
        method = "GET",
        portalUrl = "https://portal.ut.edu.vn",
        urlTemplate = "https://portal.ut.edu.vn/api/v1/lichhoc/lichTuan?date={date}",
        authType = AuthType.SESSION_CREDENTIALS_AT_RUNTIME,
        requiredParams = emptyList(),
        safetyLevel = SafetyLevel.READ_ONLY,
        confidence = 1.0,
        status = KnowledgeStatus.VERIFIED,
        source = KnowledgeSource.HAR,
        lastVerifiedAt = "2026-06-16T00:00:00Z",
        createdAt = "2026-06-16T00:00:00Z",
        updatedAt = "2026-06-16T00:00:00Z",
    )
}
