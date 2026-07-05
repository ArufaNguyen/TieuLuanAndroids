package com.example.smartcalendar.controller

import com.example.smartcalendar.common.ApiResponse
import com.example.smartcalendar.dto.reverseapi.RunKnownToolRequest
import com.example.smartcalendar.dto.reverseapi.RunKnownToolResponse
import com.example.smartcalendar.service.KnownToolRunnerService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tools")
class KnownToolController(private val service: KnownToolRunnerService) {

    @PostMapping("/by-id/{toolId}/run")
    fun runById(
        @PathVariable toolId: Int,
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: RunKnownToolRequest
    ): ApiResponse<RunKnownToolResponse> =
        ApiResponse.success(service.runById(toolId, sessionToken, request))

    @PostMapping("/{toolName}/run")
    fun run(
        @PathVariable toolName: String,
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: RunKnownToolRequest
    ): ApiResponse<RunKnownToolResponse> =
        ApiResponse.success(service.run(toolName, sessionToken, request))
}
