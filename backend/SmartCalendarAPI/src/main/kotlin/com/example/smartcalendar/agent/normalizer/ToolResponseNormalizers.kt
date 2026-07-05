package com.example.smartcalendar.agent.normalizer

import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.agent.dto.NormalizedToolResult
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

interface ToolResponseNormalizer {
    fun supports(category: String): Boolean
    fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult
}

@Service
class ToolResponseNormalizerRegistry(
    normalizers: List<ToolResponseNormalizer>,
    private val generic: GenericJsonNormalizer
) {
    private val ordered = normalizers.filterNot { it is GenericJsonNormalizer }

    fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult =
        (ordered.firstOrNull { it.supports(tool.category) } ?: generic).normalize(tool, runResult)
}

@Component
class ScheduleNormalizer(
    private val json: ObjectMapper
) : ToolResponseNormalizer {
    private val client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    override fun supports(category: String) = category.equals("SCHEDULE", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult =
        if (hasPortalScheduleRows(runResult)) {
            normalizeWithRules(runResult)
        } else {
            normalizeWithLlm(tool, runResult) ?: normalizeWithRules(runResult)
        }

    private fun normalizeWithLlm(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult? {
        if (!enabled()) return null
        val body = runResult.body ?: return null
        val requestBody = json.writeValueAsString(
            mapOf(
                "model" to model(),
                "stream" to false,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to normalizerSystemPrompt()),
                    mapOf(
                        "role" to "user",
                        "content" to json.writeValueAsString(
                            mapOf(
                                "tool" to mapOf(
                                    "id" to tool.id,
                                    "toolName" to tool.toolName,
                                    "category" to tool.category,
                                    "method" to tool.method,
                                    "urlTemplate" to tool.urlTemplate
                                ),
                                "upstream" to mapOf(
                                    "status" to runResult.status,
                                    "contentType" to runResult.contentType,
                                    "body" to body
                                )
                            )
                        ).take(24_000)
                    )
                )
            )
        )
        val headers = mutableMapOf("Content-Type" to "application/json")
        apiKey().takeIf(String::isNotBlank)?.let { headers["Authorization"] = "Bearer $it" }

        return runCatching {
            val request = HttpRequest.newBuilder(URI("${apiBase()}/chat/completions"))
                .headers(*headers.flatMap { listOf(it.key, it.value) }.toTypedArray())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return null
            val content = json.readTree(response.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText()
            toNormalizedResult(json.readTree(extractJsonObject(content)))
        }.getOrNull()
    }

    private fun normalizeWithRules(runResult: KnownToolRunResult): NormalizedToolResult {
        val rows = rows(runResult.body)
        val items = rows.map {
            mapOf(
                "date" to normalizedDate(it.text("ngayBatDauHoc", "date", "ngay")),
                "title" to it.text("tenMonHoc", "courseName", "subjectName", "title"),
                "start" to it.text("tuGio", "startTime", "start_time"),
                "end" to it.text("denGio", "endTime", "end_time"),
                "location" to it.text("tenPhong", "room", "location")
            ).filterValues { value -> !value.isNullOrBlank() }
        }
        return NormalizedToolResult("Lịch học", "${items.size} mục lịch học.", items, items.isEmpty())
    }

    private fun hasPortalScheduleRows(runResult: KnownToolRunResult): Boolean =
        rows(runResult.body).any { row ->
            row.hasNonNull("ngayBatDauHoc") && (row.hasNonNull("tuGio") || row.hasNonNull("denGio"))
        }

    private fun normalizedDate(value: String?): String? {
        val text = value?.trim()?.takeIf(String::isNotBlank) ?: return null
        return runCatching { LocalDate.parse(text).toString() }
            .recoverCatching { LocalDate.parse(text, PORTAL_DATE).toString() }
            .getOrNull()
            ?: text
    }

    private fun toNormalizedResult(node: JsonNode): NormalizedToolResult {
        val items = node.path("items")
            .takeIf(JsonNode::isArray)
            ?.map { item ->
                item.fields().asSequence()
                    .associate { it.key to primitiveText(it.value) }
                    .filterValues { value -> value != null && value.toString().isNotBlank() }
            }
            .orEmpty()
        return NormalizedToolResult(
            title = node.path("title").asText("Lịch học"),
            summary = node.path("summary").asText("${items.size} mục lịch học."),
            items = items,
            empty = node.path("empty").asBoolean(items.isEmpty())
        )
    }

    private fun normalizerSystemPrompt() = """
        You are the Smart Calendar schedule response normalizer.
        Return exactly one JSON object, no markdown.
        Schema:
        {
          "title": "string",
          "summary": "string",
          "items": [
            {
              "date": "YYYY-MM-DD",
              "title": "string",
              "start": "string",
              "end": "string",
              "location": "string|null",
              "description": "string|null"
            }
          ],
          "empty": false
        }

        Normalize any calendar/schedule/event API response into items.
        For Teamup-like responses, read events[].title, events[].start_dt, events[].end_dt,
        events[].location, events[].notes.
        date must be derived from the event start date as YYYY-MM-DD.
        Keep start/end as the original useful time or datetime string.
        If there are no events, return items=[] and empty=true.
    """.trimIndent()

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        require(start >= 0 && end > start) { "LLM response did not contain JSON object" }
        return trimmed.substring(start, end + 1)
    }

    private fun apiBase(): String =
        property("NINEROUTER_URL", "http://localhost:20128").trimEnd('/').let {
            if (it.endsWith("/v1")) it else "$it/v1"
        }

    private fun apiKey(): String = property("NINEROUTER_KEY", "")

    private fun model(): String = property(
        "NINEROUTER_NORMALIZER_MODEL",
        property("NINEROUTER_AGENT_CHAT_MODEL", "openrouter-combo")
    )

    private fun enabled(): Boolean =
        property("NINEROUTER_NORMALIZER_ENABLED", "true").equals("true", ignoreCase = true)

    private fun property(name: String, default: String): String =
        System.getProperty(name)?.takeIf(String::isNotBlank)
            ?: System.getenv(name)?.takeIf(String::isNotBlank)
            ?: default
}

@Component
class SemesterNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = category.equals("SEMESTER", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult {
        val items = rows(runResult.body).map {
            mapOf(
                "id" to it.text("id", "idDot", "maHocKy"),
                "name" to it.text("tenHocKy", "name", "hocKy", "semesterName")
            ).filterValues { value -> !value.isNullOrBlank() }
        }
        return NormalizedToolResult("Học kỳ", "${items.size} học kỳ.", items, items.isEmpty())
    }
}

@Component
class NotificationNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = category.equals("NOTIFICATION", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult {
        val items = rows(runResult.body).map {
            mapOf(
                "id" to it.text("id"),
                "title" to it.text("title", "tieude", "tieuDe", "name"),
                "date" to it.text("createdAt", "ngayTao", "date"),
                "category" to it.text("category", "tenLoai", "categoryName")
            ).filterValues { value -> !value.isNullOrBlank() }
        }
        return NormalizedToolResult("Thông báo", "${items.size} mục thông báo.", items, items.isEmpty())
    }
}

@Component
class RegisteredCoursesNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = category.equals("REGISTERED_COURSES", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult =
        courseResult("Môn đã đăng ký", runResult)
}

@Component
class RetakeCoursesNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = category.equals("RETAKE_COURSES", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult =
        courseResult("Môn cải thiện/học lại", runResult)
}

@Component
class LoginNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = category.equals("LOGIN", ignoreCase = true)

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult =
        NormalizedToolResult(
            "Login portal",
            if (runResult.status in 200..299) "Login portal thành công." else "API login trả về HTTP ${runResult.status}.",
            listOf(mapOf("status" to runResult.status)),
            false
        )
}

@Component
class GenericJsonNormalizer : ToolResponseNormalizer {
    override fun supports(category: String) = true

    override fun normalize(tool: AgentToolDescriptor, runResult: KnownToolRunResult): NormalizedToolResult {
        val body = runResult.body
        val rows = rows(body)
        val items = rows.take(10).map { node ->
            node.fields().asSequence().take(8).associate { it.key to primitiveText(it.value) }
        }
        val keys = body?.takeIf(JsonNode::isObject)?.fieldNames()?.asSequence()?.take(12)?.joinToString(", ").orEmpty()
        return NormalizedToolResult(
            tool.category,
            if (items.isNotEmpty()) "API trả về ${rows.size} item." else "API trả về JSON${if (keys.isBlank()) "." else " với keys: $keys."}",
            items,
            body == null || body.isMissingNode || body.isNull
        )
    }
}

private val PORTAL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun courseResult(title: String, runResult: KnownToolRunResult): NormalizedToolResult {
    val items = rows(runResult.body).map {
        mapOf(
            "class" to it.text("maLopHocPhan", "maLhp", "classCode"),
            "course" to it.text("tenHocPhan", "tenMonHoc", "courseName"),
            "credits" to it.text("soTinChi", "tinChi", "credits"),
            "status" to it.text("trangThai", "status")
        ).filterValues { value -> !value.isNullOrBlank() }
    }
    return NormalizedToolResult(title, "${items.size} môn.", items, items.isEmpty())
}

private fun rows(root: JsonNode?): List<JsonNode> {
    if (root == null || root.isNull || root.isMissingNode) return emptyList()
    val body = root.path("body").takeUnless(JsonNode::isMissingNode) ?: root
    val data = body.path("data").takeUnless(JsonNode::isMissingNode) ?: body
    val content = data.path("content").takeUnless(JsonNode::isMissingNode) ?: data
    return when {
        content.isArray -> content.toList()
        content.isObject -> listOf(content)
        else -> emptyList()
    }
}

private fun JsonNode.text(vararg names: String): String? =
    names.firstNotNullOfOrNull { name ->
        path(name).takeIf { it.isValueNode }?.asText()?.takeIf(String::isNotBlank)
    }

private fun primitiveText(node: JsonNode): Any? = when {
    node.isNull || node.isMissingNode -> null
    node.isNumber -> node.numberValue()
    node.isBoolean -> node.booleanValue()
    node.isTextual -> node.asText()
    node.isArray -> "array(${node.size()})"
    node.isObject -> "object"
    else -> node.asText()
}
