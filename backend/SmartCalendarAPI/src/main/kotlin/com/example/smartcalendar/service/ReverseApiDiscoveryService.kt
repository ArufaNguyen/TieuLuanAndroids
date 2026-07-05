package com.example.smartcalendar.service

import com.example.smartcalendar.dto.reverseapi.DiscoveryJobResponse
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.DiscoveryJob
import com.example.smartcalendar.repository.DiscoveryJobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile

@Service
class ReverseApiDiscoveryService(
    private val jobRepository: DiscoveryJobRepository,
    private val authService: AuthService,
    private val processor: ReverseApiDiscoveryProcessor,
    private val json: ObjectMapper,
    private val reverseApiProperties: ReverseApiProperties
) {
    fun create(
        file: MultipartFile,
        sessionToken: String?,
        requestedUserId: Int?,
        requireManualApprovalForPostReplay: Boolean = true,
        manualApprovedEndpointIds: Set<String> = emptySet()
    ): DiscoveryJobResponse {
        validate(file)
        val session = authService.getValidSession(sessionToken)
            ?: throw ApiException(401, "valid session is required for custom HAR upload")
        val account = session.account ?: throw ApiException(401, "valid session is required for custom HAR upload")
        val user = account.user ?: throw ApiException(401, "valid session is required for custom HAR upload")
        if (requestedUserId != null && requestedUserId != user.id) {
            throw ApiException(403, "user_id does not match the active session")
        }
        val raw = file.bytes.toString(Charsets.UTF_8)
        validateHar(raw)
        val job = jobRepository.save(DiscoveryJob(user = user, fileName = file.originalFilename))
        processor.process(job.id, raw, requireManualApprovalForPostReplay, manualApprovedEndpointIds)
        return toResponse(job)
    }

    fun get(id: String): DiscoveryJobResponse =
        jobRepository.findById(id).orElseThrow { ApiException(404, "discovery job not found") }.let(::toResponse)

    fun getAll(userId: Int?): List<DiscoveryJobResponse> =
        (userId?.let(jobRepository::findAllByUserIdOrderByCreatedAtDesc) ?: jobRepository.findAll())
            .sortedByDescending(DiscoveryJob::createdAt)
            .map(::toResponse)

    private fun validate(file: MultipartFile) {
        if (file.isEmpty) throw ApiException(400, "HAR file is required")
        if (file.size > reverseApiProperties.maxFileSize.toBytes()) {
            throw ApiException(413, "HAR file exceeds ${reverseApiProperties.maxFileSize}")
        }
        if (!file.originalFilename.orEmpty().endsWith(".har", ignoreCase = true)) {
            throw ApiException(400, "file must use the .har extension")
        }
    }

    private fun validateHar(raw: String) {
        val entries = runCatching { json.readTree(raw).path("log").path("entries") }.getOrNull()
        if (entries?.isArray != true) throw ApiException(400, "invalid HAR: log.entries must be an array")
    }

    private fun toResponse(job: DiscoveryJob) = DiscoveryJobResponse(
        id = job.id,
        userId = job.user?.id,
        fileName = job.fileName,
        status = job.status,
        result = job.resultJson?.let { runCatching { json.readTree(it) }.getOrNull() },
        errorMessage = job.errorMessage,
        createdAt = job.createdAt,
        completedAt = job.completedAt
    )
}

data class ReverseApiProperties(val maxFileSize: DataSize)
