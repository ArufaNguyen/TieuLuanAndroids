package com.example.smartcalendar.agent.controller

import com.example.smartcalendar.agent.dto.AgentChatRequest
import com.example.smartcalendar.agent.dto.AgentChatResponse
import com.example.smartcalendar.agent.service.AgentChatService
import com.example.smartcalendar.common.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/agent")
class AgentChatController(
    private val service: AgentChatService
) {
    @PostMapping("/chat")
    fun chat(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: AgentChatRequest
    ): ApiResponse<AgentChatResponse> =
        ApiResponse.success(service.chat(sessionToken, request.message, request.toolId, request.confirmed))
}
