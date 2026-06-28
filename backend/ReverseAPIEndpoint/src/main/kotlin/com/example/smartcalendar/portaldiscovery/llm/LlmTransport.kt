package com.example.smartcalendar.portaldiscovery.llm

data class LlmHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val body: String,
)

data class LlmHttpResponse(val status: Int, val body: String)

fun interface LlmTransport {
    suspend fun execute(request: LlmHttpRequest): LlmHttpResponse
}
