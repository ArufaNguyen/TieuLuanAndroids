package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.AgentPlan
import com.example.smartcalendar.agent.dto.AgentToolDescriptor
import com.example.smartcalendar.agent.dto.KnownToolRunResult
import com.example.smartcalendar.agent.dto.NormalizedToolResult
import com.example.smartcalendar.agent.util.DateParamResolver
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ResponseComposer(
    private val dateParamResolver: DateParamResolver
) {
    fun compose(
        userMessage: String,
        tool: AgentToolDescriptor,
        plan: AgentPlan,
        runResult: KnownToolRunResult,
        normalized: NormalizedToolResult
    ): String {
        if (runResult.status == 401 || runResult.status == 403) {
            return "Phiên đăng nhập portal đã hết hạn hoặc không hợp lệ. Bạn cần đăng nhập lại."
        }
        if (runResult.status == 400) {
            return "Portal trả về lỗi 400. Có thể tham số chưa đúng hoặc còn thiếu."
        }
        if (runResult.status == 404) {
            return "API portal không trả về dữ liệu hoặc endpoint có thể đã thay đổi."
        }
        if (runResult.status >= 500) {
            return "Portal server đang trả về lỗi ${runResult.status}. Bạn thử lại sau nhé."
        }
        if (normalized.empty) {
            return "Tôi gọi được API ${tool.category}, nhưng không thấy dữ liệu phù hợp."
        }
        if (normalized.items.isEmpty()) {
            return "Tôi gọi được API và nhận được dữ liệu: ${normalized.summary}"
        }
        return when (tool.category.uppercase()) {
            "SCHEDULE" -> scheduleAnswer(userMessage, normalized)
            "NOTIFICATION" -> listAnswer("Thông báo", normalized)
            "SEMESTER" -> listAnswer("Các học kỳ", normalized)
            "REGISTERED_COURSES" -> listAnswer("Các môn đã đăng ký", normalized)
            "RETAKE_COURSES" -> listAnswer("Các môn cải thiện/học lại", normalized)
            "LOGIN" -> normalized.summary
            else -> "Tôi gọi được API và nhận được dữ liệu sau:\n" + bulletItems(normalized.items)
        }
    }

    private fun scheduleAnswer(userMessage: String, result: NormalizedToolResult): String {
        val range = dateParamResolver.resolveDateRange(userMessage)
        val items = range?.let { (start, end) ->
            result.items.filter { item ->
                val date = item["date"]?.toString()?.let(::parseDate)
                date != null && !date.isBefore(start) && !date.isAfter(end)
            }
        } ?: result.items

        if (items.isEmpty()) {
            return range?.let { (start, end) ->
                "Không thấy lịch học trong khoảng ${DISPLAY.format(start)} - ${DISPLAY.format(end)}."
            } ?: "Không thấy lịch học phù hợp."
        }

        val prefix = range?.let { (start, end) ->
            "Lịch học trong khoảng ${DISPLAY.format(start)} - ${DISPLAY.format(end)}:"
        } ?: "Lịch học tìm thấy:"

        return prefix + "\n" + items
            .sortedWith(
                compareBy<Map<String, Any?>> { it["date"]?.toString()?.let(::parseDate) ?: LocalDate.MAX }
                    .thenBy { it["start"]?.toString().orEmpty() }
            )
            .joinToString("\n") { item ->
                val date = item["date"]?.toString().orEmpty()
                val time = listOfNotNull(item["start"], item["end"]).joinToString(" - ")
                val title = item["title"]?.toString().orEmpty()
                val room = item["location"]?.toString()?.takeIf(String::isNotBlank)?.let { " tại $it" }.orEmpty()
                "- ${listOf(date, time).filter(String::isNotBlank).joinToString(" ")}: $title$room"
            }
    }

    private fun listAnswer(title: String, result: NormalizedToolResult): String =
        "$title:\n" + bulletItems(result.items)

    private fun bulletItems(items: List<Map<String, Any?>>): String =
        items.take(10).joinToString("\n") { item ->
            "- " + item.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }

    private fun parseDate(value: String): LocalDate? {
        val normalized = value.trim()
        val isoDatePrefix = normalized.take(10).takeIf {
            it.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        }
        return listOfNotNull(normalized, isoDatePrefix).firstNotNullOfOrNull { candidate ->
            listOf(ISO, DISPLAY).firstNotNullOfOrNull { formatter ->
                runCatching { LocalDate.parse(candidate, formatter) }.getOrNull()
            }
        }
    }

    companion object {
        private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
        private val DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
