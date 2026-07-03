package com.example.smartcalendar.service

import com.example.smartcalendar.model.DiscoveryJobStatus
import com.example.smartcalendar.portaldiscovery.DesktopDiscoveryRequest
import com.example.smartcalendar.portaldiscovery.runDesktopDiscovery
import com.example.smartcalendar.repository.DatabaseApiKnowledgeRepositoryFactory
import com.example.smartcalendar.repository.DiscoveryJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReverseApiDiscoveryProcessor(
    private val jobRepository: DiscoveryJobRepository,
    private val knowledgeRepositoryFactory: DatabaseApiKnowledgeRepositoryFactory,
    private val json: ObjectMapper
) {
    @Async
    fun process(
        jobId: String,
        harRawJson: String,
        requireManualApprovalForPostReplay: Boolean = true,
        manualApprovedEndpointIds: Set<String> = emptySet()
    ) {
        val job = jobRepository.findById(jobId).orElse(null) ?: return
        try {
            val result = runBlocking {
                runDesktopDiscovery(
                    request = DesktopDiscoveryRequest(
                        harRawJson = harRawJson,
                        requireManualApprovalForPostReplay = requireManualApprovalForPostReplay,
                        manualApprovedEndpointIds = manualApprovedEndpointIds
                    ),
                    knowledgeRepository = knowledgeRepositoryFactory.create(job.user, job)
                )
            }
            job.status = DiscoveryJobStatus.COMPLETED
            job.resultJson = json.writeValueAsString(result)
            job.completedAt = LocalDateTime.now()
            jobRepository.save(job)
        } catch (exception: Exception) {
            job.status = DiscoveryJobStatus.FAILED
            job.errorMessage = exception.message ?: "Discovery processing failed."
            job.completedAt = LocalDateTime.now()
            jobRepository.save(job)
        }
    }
}
