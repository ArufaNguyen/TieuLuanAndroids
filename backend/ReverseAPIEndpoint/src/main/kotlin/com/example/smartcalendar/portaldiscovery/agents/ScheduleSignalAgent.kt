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
        val responseBody = endpoint.sample.responseBody.orEmpty()
        val raw = llmClient.complete(
            baseUrl = config.nineRouterUrl,
            key = config.nineRouterKey,
            models = listOf(signalModel()),
            agent = "Signal",
            schema = """{"candidate":true,"category":"LOGIN|SCHEDULE|REGISTERED_COURSES|AVAILABLE_COURSES|RETAKE_COURSES|NOTIFICATION|SEMESTER|DANGEROUS_WRITE|OTHER","confidence":0.0,"reason":"string"}""",
            input = """
                Decide whether this captured HTTP endpoint is a useful portal read endpoint.

                IMPORTANT:
                Read the responseSample/body first. Do not classify by URL alone.
                The response body is the strongest evidence from the HAR.
                If responseSample contains arrays/objects that look like calendar events,
                schedules, courses, notifications, or semesters, use that evidence even if
                the URL is generic.

                Return candidate=true only for endpoints that likely perform portal login or fetch useful
                user-facing data: schedule/calendar/events/timetable, registered courses, available courses,
                retake courses, notifications, semesters, or dangerous writes.

                Prefer LOGIN when the URL, request, or response is clearly an authentication endpoint,
                such as /login, /user/login, password/username credentials, access_token, bearer token,
                or a login response that returns session credentials.

                Prefer SCHEDULE when the response contains event-like records with fields such as:
                title, name, subject, course, start, end, startDate, endDate, start_dt, end_dt,
                date, time, room, location, all_day, rrule, events[].
                Teamup-like responses with events[].title + events[].start_dt + events[].end_dt
                are SCHEDULE even if the URL is only /events.

                Prefer NOTIFICATION only when the response is clearly announcement/news/message data,
                not merely because it has title/content fields.

                Endpoint snapshot:
                ${endpoint.snapshot(json)}

                Response evidence summary:
                ${json.writeValueAsString(responseEvidence(responseBody))}
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

    private fun responseEvidence(responseBody: String): Map<String, Any?> {
        val lower = responseBody.lowercase()
        return mapOf(
            "hasResponseBody" to responseBody.isNotBlank(),
            "responseLength" to responseBody.length,
            "hasEventsArray" to lower.contains("\"events\""),
            "hasCalendarTimeFields" to listOf(
                "\"start_dt\"",
                "\"end_dt\"",
                "\"start\"",
                "\"end\"",
                "startdate",
                "enddate",
                "tugio",
                "dengio",
                "ngaybatdauhoc"
            ).any(lower::contains),
            "hasTitleField" to listOf("\"title\"", "tenmonhoc", "coursename", "subjectname").any(lower::contains),
            "hasLocationField" to listOf("\"location\"", "tenphong", "room", "phonghoc").any(lower::contains),
            "hasRecurrenceField" to listOf("\"rrule\"", "\"all_day\"").any(lower::contains),
            "hasLoginEvidence" to listOf("access_token", "refresh_token", "\"token\"", "\"username\"", "\"password\"", "bearer").any(lower::contains)
        )
    }

    private fun signalModel(): String =
        System.getProperty("NINEROUTER_SIGNAL_MODEL")
            ?: System.getenv("NINEROUTER_SIGNAL_MODEL")
            ?: config.nineRouterPalamedesModel

    private fun heuristicDetect(endpoint: Endpoint): SignalResult {
        val url = endpoint.url.lowercase()
        val body = endpoint.sample.responseBody.orEmpty().lowercase()
        if (isLoginEndpoint(url, endpoint.sample.requestBody.orEmpty().lowercase(), body)) {
            return SignalResult(true, 0.98, EndpointCategory.LOGIN)
        }
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
            EndpointCategory.LOGIN to setOf("/login", "/user/login", "/auth/login", "/signin", "/sign-in", "/token"),
            EndpointCategory.RETAKE_COURSES to setOf("hoclai", "hoc-lai", "caithien", "cai-thien", "retake"),
            EndpointCategory.REGISTERED_COURSES to setOf("dadangky", "da-dang-ky", "registered", "hocphandadangky", "lhpdadangky"),
            EndpointCategory.SCHEDULE to setOf("schedule", "timetable", "lichhoc", "lich-hoc", "thoikhoabieu", "tkb"),
            EndpointCategory.NOTIFICATION to setOf("notification", "thongbao", "thong-bao", "getnote", "getpopup"),
            EndpointCategory.SEMESTER to setOf("hocky", "hoc-ky", "semester"),
            EndpointCategory.AVAILABLE_COURSES to setOf("hocphanhocmoi", "hoc-phan-hoc-moi", "course-registration"),
        )

        val CATEGORY_SIGNALS = mapOf(
            EndpointCategory.LOGIN to (
                setOf("/login", "/user/login", "/auth/login", "/signin", "/sign-in", "/token") to
                    setOf("access_token", "refresh_token", "\"token\"", "\"username\"", "\"password\"", "bearer")
                ),
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

        private fun isLoginEndpoint(url: String, requestBody: String, responseBody: String): Boolean {
            val urlLooksLogin = listOf("/login", "/user/login", "/auth/login", "/signin", "/sign-in").any(url::contains)
            val credentialRequest = listOf("username", "password", "grant_type", "recaptcha").any(requestBody::contains)
            val credentialResponse = listOf("access_token", "refresh_token", "\"token\"", "bearer").any(responseBody::contains)
            return urlLooksLogin || (credentialRequest && credentialResponse)
        }
    }
}
