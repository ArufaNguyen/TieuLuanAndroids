package com.example.smartcalendar.portaldiscovery.llm

import com.fasterxml.jackson.databind.ObjectMapper

class OpenAiCompatibleLlmClient(
    private val json: ObjectMapper,
    private val transport: LlmTransport = DesktopJavaLlmTransport(),
) {
    suspend fun complete(baseUrl: String, key: String?, models: List<String>, agent: String, schema: String, input: String): String {
        var last: Exception? = null
        for (model in models.distinct()) {
            try {
                val body = json.writeValueAsString(mapOf("model" to model, "stream" to false, "messages" to listOf(
                    mapOf("role" to "system", "content" to "$agent analyzes only. Return exactly one JSON object matching: $schema"),
                    mapOf("role" to "user", "content" to input.take(24_000)),
                )))
                val headers = mutableMapOf("Content-Type" to "application/json")
                if (!key.isNullOrBlank()) headers["Authorization"] = "Bearer $key"
                val response = transport.execute(LlmHttpRequest("${apiBase(baseUrl)}/chat/completions", headers, body))
                if (response.status in 200..299) return json.readTree(response.body).path("choices").path(0).path("message").path("content").asText()
                last = IllegalStateException("HTTP ${response.status}")
            } catch (exception: Exception) { last = exception }
        }
        throw IllegalStateException("$agent failed across configured models: ${last?.message}", last)
    }

    private fun apiBase(baseUrl: String) = baseUrl.trimEnd('/').let { if (it.endsWith("/v1")) it else "$it/v1" }
}
