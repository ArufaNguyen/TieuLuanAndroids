package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.reverseapi.DiscoveryJobResponse
import com.example.smartcalendar.service.ReverseApiDiscoveryService
import com.example.smartcalendar.service.SessionAuthorizationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/analyze")
class ReverseApiController(
    private val service: ReverseApiDiscoveryService,
    private val authorization: SessionAuthorizationService
) {

    @PostMapping(value = ["", "/"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @RequestPart("file") file: MultipartFile,
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestParam(name = "user_id", required = false) userIdSnake: Int?,
        @RequestParam(name = "userId", required = false) userIdCamel: Int?,
        @RequestParam(name = "require_manual_approval_for_post_replay", required = false) requireManualApprovalSnake: Boolean?,
        @RequestParam(name = "requireManualApprovalForPostReplay", required = false) requireManualApprovalCamel: Boolean?,
        @RequestParam(name = "manual_approved_endpoint_ids", required = false) manualApprovedEndpointIdsSnake: String?,
        @RequestParam(name = "manualApprovedEndpointIds", required = false) manualApprovedEndpointIdsCamel: String?
    ): ApiResponse<DiscoveryJobResponse> =
        ApiResponse.accepted(
            service.create(
                file = file,
                sessionToken = sessionToken,
                requestedUserId = userIdSnake ?: userIdCamel,
                requireManualApprovalForPostReplay = requireManualApprovalSnake ?: requireManualApprovalCamel ?: true,
                manualApprovedEndpointIds = parseEndpointIds(manualApprovedEndpointIdsSnake ?: manualApprovedEndpointIdsCamel)
            )
        )

    @GetMapping(value = ["", "/"])
    fun getAll(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestParam(name = "user_id", required = false) userIdSnake: Int?,
        @RequestParam(name = "userId", required = false) userIdCamel: Int?
    ): ApiResponse<List<DiscoveryJobResponse>> {
        val userId = authorization.requireUser(sessionToken, userIdSnake ?: userIdCamel).id
        return ApiResponse.success(service.getAll(userId))
    }

    @GetMapping("/{id}")
    fun get(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable id: String
    ): ApiResponse<DiscoveryJobResponse> {
        val result = service.get(id)
        authorization.requireUser(sessionToken, result.userId)
        return ApiResponse.success(result)
    }

    private fun parseEndpointIds(raw: String?): Set<String> =
        raw.orEmpty()
            .split('\n', ',', ';')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
}
