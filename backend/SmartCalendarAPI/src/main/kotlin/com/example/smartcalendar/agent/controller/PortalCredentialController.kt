package com.example.smartcalendar.agent.controller

import com.example.smartcalendar.agent.dto.CompletePortalCredentialCaptureRequest
import com.example.smartcalendar.agent.dto.PortalCredentialCaptureResponse
import com.example.smartcalendar.agent.dto.StartPortalCredentialCaptureRequest
import com.example.smartcalendar.agent.service.PortalCredentialCaptureService
import com.example.smartcalendar.common.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/portal-credentials")
class PortalCredentialController(
    private val service: PortalCredentialCaptureService
) {
    @PostMapping("/capture/start")
    fun start(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: StartPortalCredentialCaptureRequest
    ): ApiResponse<PortalCredentialCaptureResponse> =
        ApiResponse.success(service.start(sessionToken, request))

    @PostMapping("/capture/{captureId}/complete")
    fun complete(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @PathVariable captureId: String,
        @RequestBody request: CompletePortalCredentialCaptureRequest
    ): ApiResponse<PortalCredentialCaptureResponse> =
        ApiResponse.success(service.complete(sessionToken, captureId, request))

    @GetMapping("/me")
    fun current(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?
    ): ApiResponse<PortalCredentialCaptureResponse> =
        ApiResponse.success(service.current(sessionToken))
}

