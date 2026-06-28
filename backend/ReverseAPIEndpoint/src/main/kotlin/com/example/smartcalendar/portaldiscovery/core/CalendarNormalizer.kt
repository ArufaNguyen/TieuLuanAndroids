package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CalendarEventDto
import com.example.smartcalendar.portaldiscovery.CalendarMapping
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CalendarNormalizer(private val json: ObjectMapper) {
    fun normalize(body: String, mapping: CalendarMapping): List<CalendarEventDto> {
        val root = runCatching { json.readTree(body) }.getOrNull() ?: return emptyList()
        return rows(root).mapNotNull { row ->
            val title = row.value(mapping.title) ?: return@mapNotNull null
            val start = temporal(row, mapping.date, mapping.start) ?: return@mapNotNull null
            val end = temporal(row, mapping.date, mapping.end) ?: return@mapNotNull null
            CalendarEventDto(
                title,
                start,
                end,
                mapping.location?.let { row.value(it) },
                mapping.description?.let { row.value(it) },
            )
        }
    }

    private fun rows(root: JsonNode): List<JsonNode> {
        val payload = root.path("body").takeUnless(JsonNode::isMissingNode) ?: root
        return if (payload.isArray) payload.toList() else listOf(payload)
    }

    private fun temporal(row: JsonNode, dateField: String?, timeField: String): String? {
        val time = row.value(timeField) ?: return null
        if (dateField == null || time.contains('T')) return time
        val date = row.value(dateField) ?: return null
        return runCatching {
            LocalDateTime.of(
                LocalDate.parse(date, DATE_FORMAT),
                LocalTime.parse(time, TIME_FORMAT),
            ).toString()
        }.getOrNull()
    }

    private fun JsonNode.value(field: String): String? = path(field).takeIf(JsonNode::isValueNode)?.asText()?.takeIf(String::isNotBlank)

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm")
    }
}
