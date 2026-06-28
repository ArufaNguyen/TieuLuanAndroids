package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.PortalDiscoveryConfig
import com.example.smartcalendar.portaldiscovery.EndpointCategory
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.SignalResult
import com.example.smartcalendar.portaldiscovery.llm.OpenAiCompatibleLlmClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking

class ScheduleSignalAgent(
    private val json: ObjectMapper = jacksonObjectMapper(),
    private val config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
    private val llmClient: OpenAiCompatibleLlmClient = OpenAiCompatibleLlmClient(json),
) {
    fun detect(endpoint: Endpoint): SignalResult =
        runCatching { detectWithLlm(endpoint) }
            .getOrElse { heuristicDetect(endpoint) }

    private fun detectWithLlm(endpoint: Endpoint): SignalResult = runBlocking {
        val raw = llmClient.complete(
            baseUrl = config.nineRouterUrl,
            key = config.nineRouterKey,
            models = listOf(signalModel()),
            agent = "Signal",
            schema = """{"candidate":true,"category":"SCHEDULE|REGISTERED_COURSES|AVAILABLE_COURSES|RETAKE_COURSES|NOTIFICATION|SEMESTER|DANGEROUS_WRITE|OTHER","confidence":0.0,"reason":"string"}""",
            input = """
                Decide whether this captured HTTP endpoint is a useful portal read endpoint.

                Return candidate=true only for endpoints that likely fetch useful user-facing data:
                schedule/calendar/events/timetable, registered courses, available courses,
                retake courses, notifications, semesters, or dangerous writes.

                Prefer SCHEDULE for calendar/event endpoints, especially URLs with date ranges
                such as startDate/endDate, from/to, begin/end, week, month, calendar, events.

                Endpoint:
                ${endpoint.snapshot(json)}
            """.trimIndent(),
        )
        val node = json.readTree(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
        val category = EndpointCategory.entries.firstOrNull {
            it.name == node.path("category").asText().uppercase()
        } ?: EndpointCategory.OTHER
        val confidence = node.path("confidence").asDouble(0.0).coerceIn(0.0, 1.0)
        val candidate = node.path("candidate").asBoolean(false) &&
            category != EndpointCategory.OTHER &&
            category != EndpointCategory.DANGEROUS_WRITE
        SignalResult(candidate, confidence, category)
    }

    private fun signalModel(): String =
        System.getProperty("NINEROUTER_SIGNAL_MODEL")
            ?: System.getenv("NINEROUTER_SIGNAL_MODEL")
            ?: config.nineRouterPalamedesModel

    private fun heuristicDetect(endpoint: Endpoint): SignalResult {
        val url = endpoint.url.lowercase()
        val body = endpoint.sample.responseBody.orEmpty().lowercase()
        val specificCategory = SPECIFIC_URL_SIGNALS.entries.firstOrNull { (_, words) -> words.any(url::contains) }?.key
        val scored = CATEGORY_SIGNALS.mapValues { (_, signals) ->
            val urlHits = signals.first.count(url::contains)
            val bodyHits = signals.second.count(body::contains)
            ((if (urlHits > 0) 0.55 else 0.0) + bodyHits * 0.09).coerceAtMost(1.0)
        }
        if (specificCategory != null) {
            val confidence = scored.getValue(specificCategory).coerceAtLeast(0.55)
            return SignalResult(confidence >= 0.45, confidence, specificCategory)
        }
        val best = scored.maxByOrNull { it.value }
        if (best == null || best.value == 0.0) {
            return SignalResult(false, 0.0, EndpointCategory.OTHER)
        }
        return SignalResult(
            candidate = best.value >= 0.45,
            confidence = best.value,
            suggestedCategory = best.key,
        )
    }

    companion object {
        val SPECIFIC_URL_SIGNALS = linkedMapOf(
            EndpointCategory.RETAKE_COURSES to setOf("hoclai", "hoc-lai", "caithien", "cai-thien", "retake"),
            EndpointCategory.REGISTERED_COURSES to setOf("dadangky", "da-dang-ky", "registered", "hocphandadangky", "lhpdadangky"),
            EndpointCategory.SCHEDULE to setOf("schedule", "timetable", "lichhoc", "lich-hoc", "thoikhoabieu", "tkb"),
            EndpointCategory.NOTIFICATION to setOf("notification", "thongbao", "thong-bao", "getnote", "getpopup"),
            EndpointCategory.SEMESTER to setOf("hocky", "hoc-ky", "semester"),
            EndpointCategory.AVAILABLE_COURSES to setOf("hocphanhocmoi", "hoc-phan-hoc-moi", "course-registration"),
        )

        val CATEGORY_SIGNALS = mapOf(
            EndpointCategory.SCHEDULE to (
                setOf("schedule", "timetable", "calendar", "lichhoc", "lich-hoc", "thoikhoabieu", "tkb") to
                    setOf("coursename", "tenmonhoc", "teacher", "giangvien", "room", "phonghoc", "weekday", "startperiod")
                ),
            EndpointCategory.NOTIFICATION to (
                setOf("notification", "thongbao", "thong-bao", "getnote", "getpopup") to
                    setOf("notification", "thongbao", "tieude", "title", "content", "noidung")
                ),
            EndpointCategory.REGISTERED_COURSES to (
                setOf("dadangky", "da-dang-ky", "registered", "hocphandadangky") to
                    setOf("dadangky", "registered", "malophocphan", "tenhocphan")
                ),
            EndpointCategory.RETAKE_COURSES to (
                setOf("hoclai", "hoc-lai", "caithien", "cai-thien", "retake") to
                    setOf("hoclai", "caithien", "retake")
                ),
            EndpointCategory.AVAILABLE_COURSES to (
                setOf("hocphan", "hoc-phan", "course-registration", "dangkyhocphan", "dang-ky-hoc-phan", "/dkhp/") to
                    setOf("malophocphan", "tenhocphan", "siso", "conlai", "available")
                ),
            EndpointCategory.SEMESTER to (
                setOf("hocky", "hoc-ky", "semester") to setOf("hocky", "semester")
                ),
        )
    }
}
