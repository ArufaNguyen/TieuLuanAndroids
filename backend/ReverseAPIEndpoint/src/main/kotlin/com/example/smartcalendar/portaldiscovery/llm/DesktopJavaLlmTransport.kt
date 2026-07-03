package com.example.smartcalendar.portaldiscovery.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DesktopJavaLlmTransport : LlmTransport {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    override suspend fun execute(request: LlmHttpRequest): LlmHttpResponse = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI(request.url)).timeout(Duration.ofSeconds(90))
        request.headers.forEach(builder::header)
        val response = client.send(
            builder.POST(HttpRequest.BodyPublishers.ofString(request.body)).build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        LlmHttpResponse(response.statusCode(), response.body())
    }
}
