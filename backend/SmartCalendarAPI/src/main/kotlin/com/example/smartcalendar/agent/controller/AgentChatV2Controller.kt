package com.example.smartcalendar.agent.controller

import com.example.smartcalendar.agent.dto.AgentChatResponse
import com.example.smartcalendar.agent.dto.AgentChatV2Request
import com.example.smartcalendar.agent.service.AgentChatService
import com.example.smartcalendar.common.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/agent")
class AgentChatV2Controller(
    private val service: AgentChatService
) {
    @PostMapping("/chat")
    fun chat(
        @RequestHeader("X-Session-Token", required = false) sessionToken: String?,
        @RequestBody request: AgentChatV2Request
    ): ApiResponse<AgentChatResponse> =
        ApiResponse.success(service.chatV2(sessionToken, request.message, request.confirmed))
}
