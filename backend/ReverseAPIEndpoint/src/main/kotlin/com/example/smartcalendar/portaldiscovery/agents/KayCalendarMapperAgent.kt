package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class KayCalendarMapperAgent(private val router: AgentLlmRouter, private val json: ObjectMapper) {
    suspend fun map(responseBody: String): CalendarMapping {
        infer(responseBody)?.let { return it }
        val raw = router.kay(
            """{"title":"field","date":"field|null","start":"field","end":"field","location":"field|null","description":"field|null","confidence":0.0}""",
            responseBody,
        )
        val node = json.readTree(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
        return CalendarMapping(
            title = node.path("title").asText(),
            start = node.path("start").asText(),
            end = node.path("end").asText(),
            location = node.path("location").textOrNull(),
            description = node.path("description").textOrNull(),
            confidence = node.path("confidence").asDouble(0.0),
            verified = false,
            date = node.path("date").textOrNull(),
        )
    }

    fun infer(responseBody: String): CalendarMapping? {
        val root = runCatching { json.readTree(responseBody) }.getOrNull() ?: return null
        val payload = root.path("body").takeUnless(JsonNode::isMissingNode) ?: root
        val row = if (payload.isArray) payload.firstOrNull() else payload
        if (row == null || !row.isObject) return null
        val fields = row.fieldNames().asSequence().associateBy(String::lowercase)
        fun field(vararg aliases: String) = aliases.firstNotNullOfOrNull { fields[it.lowercase()] }
        val title = field("title", "courseName", "subjectName", "tenMonHoc", "name") ?: return null
        val start = field("start", "startTime", "start_time", "tuGio") ?: return null
        val end = field("end", "endTime", "end_time", "denGio") ?: return null
        val date = field("date", "startDate", "ngay", "ngayBatDauHoc")
        if (date == null && !row.path(start).asText().contains('T')) return null
        return CalendarMapping(
            title = title,
            start = start,
            end = end,
            location = field("location", "room", "tenPhong"),
            description = field("description", "note", "ghiChu"),
            confidence = 0.95,
            verified = false,
            date = date,
        )
    }

    private fun JsonNode.textOrNull() = takeIf(JsonNode::isTextual)?.asText()?.takeUnless { it.equals("null", true) }
}
