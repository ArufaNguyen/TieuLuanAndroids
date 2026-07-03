package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CapturedExchange
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

class HarCaptureAdapter(private val json: ObjectMapper) {
    fun fromFile(path: String): List<CapturedExchange> = fromRawJson(Files.readString(Path.of(path)))

    fun fromRawJson(raw: String): List<CapturedExchange> {
        val entries = json.readTree(raw).path("log").path("entries")
        require(entries.isArray) { "HAR log.entries must be an array." }
        return entries.map { entry ->
            val request = entry.path("request")
            val response = entry.path("response")
            val content = response.path("content")
            val responseBody = content.path("text").takeIf(JsonNode::isTextual)?.asText()?.let {
                if (content.path("encoding").asText().equals("base64", true)) {
                    runCatching { String(Base64.getDecoder().decode(it)) }.getOrDefault(it)
                } else {
                    it
                }
            }
            val requestHeaders = request.headers().toMutableMap()
            val cookies = request.path("cookies")
                .takeIf(JsonNode::isArray)
                ?.mapNotNull { cookie ->
                    val name = cookie.path("name").asText().takeIf(String::isNotBlank) ?: return@mapNotNull null
                    "$name=${cookie.path("value").asText()}"
                }
                .orEmpty()
            if (cookies.isNotEmpty() && requestHeaders.keys.none { it.equals("cookie", true) }) {
                requestHeaders["Cookie"] = listOf(cookies.joinToString("; "))
            }
            CapturedExchange(
                method = request.path("method").asText(),
                url = request.path("url").asText(),
                requestHeaders = requestHeaders,
                requestBody = request.path("postData").path("text").takeIf(JsonNode::isTextual)?.asText(),
                responseStatus = response.path("status").takeIf(JsonNode::isInt)?.asInt(),
                responseContentType = content.path("mimeType").takeIf(JsonNode::isTextual)?.asText(),
                responseBody = responseBody,
                responseHeaders = response.headers(),
            )
        }
    }

    private fun JsonNode.headers(): Map<String, List<String>> = path("headers")
        .groupBy({ it.path("name").asText() }, { it.path("value").asText() })
}
