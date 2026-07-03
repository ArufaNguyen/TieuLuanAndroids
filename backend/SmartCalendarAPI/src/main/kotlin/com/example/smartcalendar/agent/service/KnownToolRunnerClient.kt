package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.dto.reverseapi.RunKnownToolRequest
import com.example.smartcalendar.service.KnownToolRunnerService
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

@Service
class KnownToolRunnerClient(
    private val runner: KnownToolRunnerService
) {
    fun runById(
        sessionToken: String,
        toolId: Int,
        params: Map<String, String>,
        credentials: Map<String, String>,
        headers: Map<String, String>,
        body: JsonNode?
    ): KnownToolRunResult {
        val result = runner.runById(
            toolId,
            sessionToken,
            RunKnownToolRequest(
                params = params,
                credentials = credentials,
                headers = headers,
                body = body
            )
        )
        return KnownToolRunResult(
            toolName = result.toolName,
            resolvedToolName = result.resolvedToolName,
            method = result.method,
            url = result.url,
            status = result.status,
            contentType = result.contentType,
            body = result.body,
            rawBody = result.rawBody,
            usedHeaders = result.usedHeaders
        )
    }
}

