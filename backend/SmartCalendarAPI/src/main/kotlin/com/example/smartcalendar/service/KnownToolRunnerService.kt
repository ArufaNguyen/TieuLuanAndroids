package com.example.smartcalendar.service

import com.example.smartcalendar.dto.reverseapi.RunKnownToolRequest
import com.example.smartcalendar.dto.reverseapi.RunKnownToolResponse
import com.example.smartcalendar.exception.ApiException
import com.example.smartcalendar.model.ApiKnowledgeEntity
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.repository.ApiKnowledgeJpaRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Service
class KnownToolRunnerService(
    private val authService: AuthService,
    private val repository: ApiKnowledgeJpaRepository,
    private val json: ObjectMapper
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun run(toolName: String, sessionToken: String?, request: RunKnownToolRequest): RunKnownToolResponse {
        val user = requireActiveUser(sessionToken)

        val entity = findTool(user.id, toolName)
            ?: throw ApiException(404, "known tool not found")
        return runEntity(toolName, entity, request)
    }

    fun runById(toolId: Int, sessionToken: String?, request: RunKnownToolRequest): RunKnownToolResponse {
        val user = requireActiveUser(sessionToken)
        val entity = repository.findById(toolId).orElseThrow { ApiException(404, "known tool not found") }
        val ownerId = entity.user?.id
        if (ownerId != null && ownerId != user.id) {
            throw ApiException(403, "known tool does not belong to active session")
        }
        return runEntity(entity.toolName, entity, request)
    }

    private fun requireActiveUser(sessionToken: String?) =
        (authService.getValidSession(sessionToken)
            ?: throw ApiException(401, "valid session is required to run a known tool"))
            .account?.user ?: throw ApiException(401, "valid session is required to run a known tool")

    private fun runEntity(
        requestedToolName: String,
        entity: ApiKnowledgeEntity,
        request: RunKnownToolRequest
    ): RunKnownToolResponse {
        val knowledge = json.readValue(entity.knowledgeJson, ApiKnowledge::class.java)

        if (knowledge.safetyLevel.name != "READ_ONLY" && knowledge.category.name != "LOGIN") {
            throw ApiException(403, "known tool is not read-only")
        }

        val url = fillUrl(knowledge.urlTemplate, request.params)
        val headers = buildHeaders(knowledge, request, url)
        val outbound = buildHttpRequest(knowledge.method, url, headers, request.body)
        val response = client.send(outbound, HttpResponse.BodyHandlers.ofString())
        val contentType = response.headers().firstValue("content-type").orElse(null)
        val rawBody = response.body().orEmpty()
        val parsedBody = parseBody(rawBody, contentType)

        return RunKnownToolResponse(
            toolName = requestedToolName,
            resolvedToolName = entity.toolName,
            method = knowledge.method.uppercase(),
            url = url,
            status = response.statusCode(),
            contentType = contentType,
            body = parsedBody,
            rawBody = if (parsedBody == null) rawBody else null,
            usedHeaders = headers.keys.sorted()
        )
    }

    private fun findTool(userId: Int, toolName: String) =
        repository.findFirstByUserIdAndToolNameOrderByUpdatedAtDescIdDesc(userId, toolName)
            ?: repository.findFirstByUserIdAndToolNameOrderByUpdatedAtDescIdDesc(userId, "user_${userId}_$toolName")
            ?: repository.findFirstByUserIsNullAndToolNameOrderByUpdatedAtDescIdDesc(toolName)

    private fun fillUrl(template: String, params: Map<String, String>): String {
        var url = template
        PLACEHOLDER.findAll(template).forEach { match ->
            val name = match.groupValues[1]
            val value = params[name] ?: throw ApiException(400, "missing required param: $name")
            url = url.replace("{$name}", encode(value))
        }
        return url
    }

    private fun buildHeaders(knowledge: ApiKnowledge, request: RunKnownToolRequest, url: String): Map<String, String> {
        val provided = request.credentials + request.headers
        val requiredNames = (
            knowledge.headersPolicy.requiredCredentialHeaders +
                knowledge.headersPolicy.requiredStaticHeaders
            ).distinct()

        val requiredHeaders = requiredNames.associateWith { name ->
            provided.findHeader(name)
                ?: defaultStaticHeader(name, url)
                ?: throw ApiException(400, "missing required header credential: $name")
        }.filterKeys(::isAllowedOutboundHeader)

        val extraHeaders = provided
            .filterKeys(::isAllowedOutboundHeader)
            .filterKeys { name -> requiredHeaders.keys.none { it.equals(name, ignoreCase = true) } }
            .filterValues { it.isNotBlank() }

        return requiredHeaders + extraHeaders
    }

    private fun buildHttpRequest(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: JsonNode?
    ): HttpRequest {
        val builder = HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(90))
        headers.forEach { (name, value) -> builder.header(name, value) }
        return when (method.uppercase()) {
            "GET" -> builder.GET().build()
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body?.toString().orEmpty())).build()
            "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body?.toString().orEmpty())).build()
            "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body?.toString().orEmpty())).build()
            "DELETE" -> builder.DELETE().build()
            else -> throw ApiException(400, "unsupported known tool method: $method")
        }
    }

    private fun parseBody(body: String, contentType: String?): JsonNode? {
        if (body.isBlank()) return null
        val looksJson = contentType?.contains("json", ignoreCase = true) == true ||
            body.trimStart().firstOrNull() in setOf('{', '[')
        return if (looksJson) runCatching { json.readTree(body) }.getOrNull() else null
    }

    private fun Map<String, String>.findHeader(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.takeIf { it.isNotBlank() }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun isAllowedOutboundHeader(name: String): Boolean =
        name.lowercase() !in BLOCKED_OUTBOUND_HEADERS

    private fun defaultStaticHeader(name: String, url: String): String? {
        val origin = runCatching {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        }.getOrNull().orEmpty()
        return when (name.lowercase()) {
            "accept" -> "application/json, text/plain, */*"
            "accept-language" -> "vi,en-US;q=0.9,en;q=0.8"
            "content-type", "content_type" -> "application/json"
            "origin" -> origin.takeIf(String::isNotBlank)
            "referer" -> origin.takeIf(String::isNotBlank)?.let { "$it/" }
            "user-agent" -> DEFAULT_USER_AGENT
            "sec-fetch-dest" -> "empty"
            "sec-fetch-mode" -> "cors"
            "sec-fetch-site" -> "same-origin"
            "sec-ch-ua" -> "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\""
            "sec-ch-ua-mobile" -> "?0"
            "sec-ch-ua-platform" -> "\"Windows\""
            else -> null
        }
    }

    companion object {
        private val PLACEHOLDER = Regex("""\{([^}]+)}""")
        private val BLOCKED_OUTBOUND_HEADERS = setOf("host", "content-length", "connection", "transfer-encoding")
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36"
    }
}
