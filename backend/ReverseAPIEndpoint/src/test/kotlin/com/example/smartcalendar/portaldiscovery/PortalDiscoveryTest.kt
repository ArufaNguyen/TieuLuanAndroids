package com.example.smartcalendar.portaldiscovery

import kotlinx.coroutines.runBlocking
import com.example.smartcalendar.portaldiscovery.core.CaptureImporter
import com.example.smartcalendar.portaldiscovery.core.HarCaptureAdapter
import com.example.smartcalendar.portaldiscovery.core.HeaderRequirementVerifier
import com.example.smartcalendar.portaldiscovery.core.HeaderReductionAdvisor
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.agents.GawainSafetyAgent
import com.example.smartcalendar.portaldiscovery.agents.ScheduleSignalAgent
import com.example.smartcalendar.portaldiscovery.agents.KayCalendarMapperAgent
import com.example.smartcalendar.portaldiscovery.core.CalendarNormalizer
import com.example.smartcalendar.portaldiscovery.core.PortalCredentialProvider
import com.example.smartcalendar.portaldiscovery.core.PortalHttpResponse
import com.example.smartcalendar.portaldiscovery.core.PortalHttpTransport
import com.example.smartcalendar.portaldiscovery.core.ReplayEngine
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.example.smartcalendar.portaldiscovery.llm.OpenAiCompatibleLlmClient
import com.example.smartcalendar.portaldiscovery.llm.LlmHttpResponse
import com.example.smartcalendar.portaldiscovery.llm.LlmTransport
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledgeFactory
import com.example.smartcalendar.portaldiscovery.knowledge.JsonFileApiKnowledgeRepository
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledge
import com.example.smartcalendar.portaldiscovery.knowledge.ApiParameter
import com.example.smartcalendar.portaldiscovery.knowledge.AuthType
import com.example.smartcalendar.portaldiscovery.knowledge.KnowledgeStatus
import com.example.smartcalendar.portaldiscovery.knowledge.SafetyLevel
import com.example.smartcalendar.portaldiscovery.runtime.KnownApiCallStatus
import com.example.smartcalendar.portaldiscovery.runtime.KnownApiCaller
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PortalDiscoveryTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `function validates exactly one capture source`() = runBlocking {
        val result = runDesktopDiscovery(DesktopDiscoveryRequest())

        assertFalse(result.success)
        assertEquals(DiscoveryStatus.FAILED_VALIDATION, result.status)
    }

    @Test
    fun `function filters static capture without database or LLM`() = runBlocking {
        val result = runDesktopDiscovery(
            DesktopDiscoveryRequest(
                capturedExchanges = listOf(CapturedExchange("GET", "https://portal.example/app.js")),
            ),
        )

        assertEquals(DiscoveryStatus.NO_CANDIDATE, result.status)
        assertEquals(0, result.trace.endpointCount)
    }

    @Test
    fun `neutral discovery core accepts captured exchanges directly`() = runBlocking {
        val result = runDiscovery(
            DiscoveryRequest(
                capturedExchanges = listOf(CapturedExchange("GET", "https://portal.example/app.js")),
                source = KnowledgeSource.ANDROID_WEBVIEW,
            ),
        )

        assertEquals(DiscoveryStatus.NO_CANDIDATE, result.status)
        assertEquals(0, result.trace.endpointCount)
    }

    @Test
    fun `function blocks unsafe schedule endpoint before LLM`() = runBlocking {
        val result = runDesktopDiscovery(
            DesktopDiscoveryRequest(
                capturedExchanges = listOf(
                    CapturedExchange("DELETE", "https://portal.example/api/schedule/delete", responseBody = """{"courseName":"X"}"""),
                ),
            ),
        )

        assertEquals(DiscoveryStatus.NO_CANDIDATE, result.status)
        assertEquals("BLOCKED", result.trace.candidates.single().arthurDecision)
    }

    @Test
    fun `HAR request cookies are restored as in-memory Cookie header`() {
        val exchange = CaptureImporter(jacksonObjectMapper()).import(
            DesktopDiscoveryRequest(
                harRawJson = """{"log":{"entries":[{"request":{"method":"GET","url":"https://portal/schedule","headers":[],"cookies":[{"name":"session","value":"secret"}]},"response":{"status":200,"content":{"mimeType":"application/json","text":"{}"}}}]}}""",
            ),
        ).single()

        assertEquals(listOf("session=secret"), exchange.requestHeaders["Cookie"])
    }

    @Test
    fun `HAR adapter reads response headers`() {
        val exchange = HarCaptureAdapter(jacksonObjectMapper()).fromRawJson(
            """{"log":{"entries":[{"request":{"method":"GET","url":"https://portal/schedule","headers":[]},"response":{"status":200,"headers":[{"name":"x-version","value":"2"}],"content":{"mimeType":"application/json","text":"{}"}}}]}}""",
        ).single()

        assertEquals(listOf("2"), exchange.responseHeaders["x-version"])
    }

    @Test
    fun `ReplayEngine uses fresh credentials and removes unsafe headers`() {
        var sentHeaders: Map<String, List<String>> = emptyMap()
        val transport = PortalHttpTransport { request ->
            sentHeaders = request.headers
            PortalHttpResponse(200, mapOf("content-type" to listOf("application/json")), """{"ok":true}""")
        }
        val credentials = PortalCredentialProvider { mapOf("Cookie" to listOf("fresh=session")) }
        val replay = ReplayEngine(jacksonObjectMapper(), transport, credentials).replay(
            CapturedExchange(
                "GET",
                "https://portal/api/schedule",
                requestHeaders = mapOf("Cookie" to listOf("old=session"), "Host" to listOf("portal")),
            ),
        )

        assertTrue(replay.success)
        assertEquals(listOf("fresh=session"), sentHeaders["Cookie"])
        assertFalse(sentHeaders.keys.any { it.equals("host", true) })
    }

    @Test
    fun `header verifier falls back to captured HAR response when live replay times out`() = runBlocking {
        val transport = PortalHttpTransport { throw IllegalStateException("request timed out") }
        val exchange = CapturedExchange(
            "GET",
            "https://portal.example/api/schedule?date=2026-06-10",
            requestHeaders = mapOf(
                "Authorization" to listOf("Bearer captured"),
                "Accept" to listOf("application/json"),
            ),
            responseStatus = 200,
            responseBody = """{"body":[{"tenMonHoc":"Mobile","ngayBatDauHoc":"10/06/2026","tuGio":"09:25","denGio":"11:55"}]}""",
        )

        val result = HeaderRequirementVerifier(ReplayEngine(jacksonObjectMapper(), transport)).verify(exchange)

        assertTrue(result.replay.success)
        assertEquals(setOf("Authorization"), result.requiredCredentialHeaders)
        assertTrue(result.replay.body.orEmpty().contains("tenMonHoc"))
    }

    @Test
    fun `header verifier proves Authorization alone is sufficient before asking Morgan`() = runBlocking {
        val attempts = mutableListOf<Set<String>>()
        var morganCalled = false
        val transport = PortalHttpTransport { request ->
            attempts += request.headers.keys
            if (request.headers.keys.any { it.equals("Authorization", true) }) {
                PortalHttpResponse(200, emptyMap(), """{"ok":true}""")
            } else {
                PortalHttpResponse(401, emptyMap(), "{}")
            }
        }
        val exchange = CapturedExchange(
            "GET",
            "https://portal/api/schedule",
            requestHeaders = mapOf(
                "Authorization" to listOf("Bearer secret"),
                "Cookie" to listOf("session=secret"),
                "Accept-Language" to listOf("vi"),
            ),
        )

        val morgan = HeaderReductionAdvisor { _, _ ->
            morganCalled = true
            listOf("Accept-Language", "Cookie", "Authorization")
        }
        val result = HeaderRequirementVerifier(ReplayEngine(jacksonObjectMapper(), transport), morgan).verify(exchange)

        assertTrue(result.replay.success)
        assertEquals(setOf("Authorization"), result.requiredCredentialHeaders)
        assertEquals(emptySet<String>(), result.requiredStaticHeaders)
        assertEquals(setOf("Authorization", "Cookie", "Accept-Language"), attempts[0])
        assertEquals(emptySet<String>(), attempts[1])
        assertEquals(setOf("Authorization"), attempts[2])
        assertFalse(morganCalled)
    }

    @Test
    fun `header verifier asks Morgan only when minimal credential probes fail`() = runBlocking {
        var morganCalled = false
        val transport = PortalHttpTransport { request ->
            val names = request.headers.keys.map(String::lowercase).toSet()
            if ("authorization" in names && "x-portal-client" in names) {
                PortalHttpResponse(200, emptyMap(), """{"ok":true}""")
            } else {
                PortalHttpResponse(401, emptyMap(), "{}")
            }
        }
        val exchange = CapturedExchange(
            "GET",
            "https://portal/api/schedule",
            requestHeaders = mapOf(
                "Authorization" to listOf("Bearer secret"),
                "Cookie" to listOf("session=secret"),
                "X-Portal-Client" to listOf("web"),
            ),
        )
        val morgan = HeaderReductionAdvisor { _, _ ->
            morganCalled = true
            listOf("Cookie", "X-Portal-Client", "Authorization")
        }

        val result = HeaderRequirementVerifier(ReplayEngine(jacksonObjectMapper(), transport), morgan).verify(exchange)

        assertTrue(morganCalled)
        assertEquals(setOf("Authorization"), result.requiredCredentialHeaders)
        assertEquals(setOf("x-portal-client"), result.requiredStaticHeaders)
    }

    @Test
    fun `GET registration catalog is readable but POST registration action is blocked`() {
        val gawain = GawainSafetyAgent()
        val read = Endpoint("read", "GET", "https://portal/api/dangkyhocphan/danhsach", CapturedExchange("GET", "https://portal/api/dangkyhocphan/danhsach"))
        val write = Endpoint("write", "POST", "https://portal/api/dangkyhocphan/register", CapturedExchange("POST", "https://portal/api/dangkyhocphan/register"))
        val unsafeGet = Endpoint("unsafe-get", "GET", "https://portal/api/courses/register", CapturedExchange("GET", "https://portal/api/courses/register"))

        assertEquals(true, gawain.review(read).allowed)
        assertEquals(false, gawain.review(read).blocked)
        assertEquals(true, gawain.review(write).blocked)
        assertEquals(true, gawain.review(unsafeGet).blocked)
    }

    @Test
    fun `signals detect notification and available course APIs`() {
        val signals = ScheduleSignalAgent()
        val notification = Endpoint("n", "GET", "https://portal/api/notification/getNote", CapturedExchange("GET", "https://portal/api/notification/getNote"))
        val courses = Endpoint("c", "GET", "https://portal/api/dangkyhocphan/danhsach", CapturedExchange("GET", "https://portal/api/dangkyhocphan/danhsach", responseBody = """{"maLopHocPhan":"A"}"""))
        val retake = Endpoint(
            "r",
            "GET",
            "https://portal/api/dkhp/getHocPhanCaiThien",
            CapturedExchange("GET", "https://portal/api/dkhp/getHocPhanCaiThien", responseBody = """{"maLopHocPhan":"A","tenHocPhan":"B","siSo":10}"""),
        )

        assertEquals(EndpointCategory.NOTIFICATION, signals.detect(notification).suggestedCategory)
        assertEquals(EndpointCategory.AVAILABLE_COURSES, signals.detect(courses).suggestedCategory)
        assertEquals(EndpointCategory.RETAKE_COURSES, signals.detect(retake).suggestedCategory)
    }

    @Test
    fun `unrelated endpoint signal remains other`() {
        val endpoint = Endpoint(
            "profile",
            "GET",
            "https://portal/api/student/profile",
            CapturedExchange("GET", "https://portal/api/student/profile", responseBody = """{"name":"Student"}"""),
        )

        assertEquals(EndpointCategory.OTHER, ScheduleSignalAgent().detect(endpoint).suggestedCategory)
    }

    @Test
    fun `normalizer unwraps body and combines date with time`() {
        val events = CalendarNormalizer(jacksonObjectMapper()).normalize(
            """{"body":[{"tenMonHoc":"Mobile","ngayBatDauHoc":"10/06/2026","tuGio":"09:25","denGio":"11:55","tenPhong":"B308"}]}""",
            CalendarMapping("tenMonHoc", "tuGio", "denGio", "tenPhong", null, 0.95, true, "ngayBatDauHoc"),
        )

        assertEquals("2026-06-10T09:25", events.single().start)
        assertEquals("2026-06-10T11:55", events.single().end)
    }

    @Test
    fun `Kay infers common date and time fields without LLM`() {
        val json = jacksonObjectMapper()
        val kay = KayCalendarMapperAgent(AgentLlmRouter(PortalDiscoveryConfig(), OpenAiCompatibleLlmClient(json)), json)

        val mapping = kay.infer(
            """{"body":[{"tenMonHoc":"Mobile","ngayBatDauHoc":"10/06/2026","tuGio":"09:25","denGio":"11:55","tenPhong":"B308"}]}""",
        )

        assertEquals("tenMonHoc", mapping?.title)
        assertEquals("ngayBatDauHoc", mapping?.date)
        assertEquals("tuGio", mapping?.start)
    }

    @Test
    fun `API knowledge persists without credentials and upserts by stable id`() {
        val json = jacksonObjectMapper()
        val path = temporaryDirectory.resolve("api-knowledge.json")
        val repository = JsonFileApiKnowledgeRepository(path, json)
        val factory = ApiKnowledgeFactory()
        val request = DiscoveryRequest(
            capturedExchanges = listOf(
                CapturedExchange(
                    "GET",
                    "https://portal/api/schedule?date=secret-date",
                    requestHeaders = mapOf("Cookie" to listOf("session=very-secret"), "Authorization" to listOf("Bearer very-secret")),
                ),
            ),
            source = KnowledgeSource.HAR,
        )
        val endpoint = VerifiedEndpoint(
            "GET https://portal/api/schedule?date={date}",
            EndpointCategory.SCHEDULE,
            "GET",
            "https://portal/api/schedule?date={date}",
            0.95,
        )
        val tool = ToolDefinition("get_student_schedule", endpoint.endpointId, EndpointCategory.SCHEDULE)
        val first = factory.create(listOf(tool), listOf(endpoint), request, Instant.parse("2026-06-11T00:00:00Z"))
        val second = factory.create(listOf(tool), listOf(endpoint), request, Instant.parse("2026-06-12T00:00:00Z"))

        repository.saveAll(first)
        repository.saveAll(second)

        val saved = repository.findAll().single()
        val raw = Files.readString(path)
        assertEquals(1, saved.id)
        assertEquals("https://portal", saved.portalUrl)
        assertEquals(listOf("date"), saved.requiredParams.map { it.name })
        assertEquals("2026-06-11T00:00:00Z", saved.createdAt)
        assertEquals("2026-06-12T00:00:00Z", saved.updatedAt)
        assertFalse(raw.contains("very-secret"))
        assertFalse(raw.contains("secret-date"))
    }

    @Test
    fun `legacy UUID knowledge migrates to sequential integer id`() {
        val json = jacksonObjectMapper()
        val path = temporaryDirectory.resolve("legacy-api-knowledge.json")
        Files.writeString(
            path,
            """
            [{
              "id":"cf894cf6-23ca-30a5-878e-7d18749bdd68",
              "toolName":"get_student_schedule",
              "purpose":"Get schedule",
              "category":"SCHEDULE",
              "method":"GET",
              "urlTemplate":"https://portal/api/schedule?date={date}",
              "authType":"SESSION_CREDENTIALS_AT_RUNTIME",
              "requiredParams":[],
              "optionalParams":[],
              "headersPolicy":{"credentialSource":"PortalCredentialProvider","persistedHeaders":[],"droppedHeaders":[]},
              "requestBodySchema":null,
              "responseSchema":null,
              "responseMapping":null,
              "safetyLevel":"READ_ONLY",
              "confidence":0.95,
              "status":"VERIFIED",
              "source":"HAR",
              "lastVerifiedAt":"2026-06-11T00:00:00Z",
              "createdAt":"2026-06-11T00:00:00Z",
              "updatedAt":"2026-06-11T00:00:00Z"
            }]
            """.trimIndent(),
        )
        val repository = JsonFileApiKnowledgeRepository(path, json)
        val migrated = repository.findAll().single()

        assertEquals(1, migrated.id)
        assertEquals("https://portal", migrated.portalUrl)
        repository.saveAll(listOf(migrated.copy(updatedAt = "2026-06-12T00:00:00Z")))
        assertFalse(Files.readString(path).contains("cf894cf6"))
    }

    @Test
    fun `knowledge repository filters same tool by portal URL`() {
        val json = jacksonObjectMapper()
        val repository = JsonFileApiKnowledgeRepository(temporaryDirectory.resolve("multi-portal.json"), json)
        val factory = ApiKnowledgeFactory()
        val request = DiscoveryRequest(emptyList(), KnowledgeSource.MANUAL)
        fun knowledge(portal: String) = factory.create(
            listOf(ToolDefinition("get_student_schedule", "schedule", EndpointCategory.SCHEDULE)),
            listOf(VerifiedEndpoint("schedule", EndpointCategory.SCHEDULE, "GET", "$portal/api/schedule?date={date}", 0.95)),
            request,
        ).single()

        repository.saveAll(listOf(knowledge("https://portal-a.edu.vn"), knowledge("https://portal-b.edu.vn")))

        assertEquals(2, repository.findAll().size)
        assertEquals(
            "https://portal-b.edu.vn",
            repository.findByPortalAndTool("https://portal-b.edu.vn", "get_student_schedule")?.portalUrl,
        )
    }

    @Test
    fun `KnownApiCaller runs verified schedule knowledge with fresh session`() {
        var requestedUrl = ""
        var requestedHeaders: Map<String, List<String>> = emptyMap()
        val transport = PortalHttpTransport { request ->
            requestedUrl = request.url
            requestedHeaders = request.headers
            PortalHttpResponse(
                200,
                emptyMap(),
                """{"body":[{"tenMonHoc":"Mobile","ngayBatDauHoc":"10/06/2026","tuGio":"09:25","denGio":"11:55"}]}""",
            )
        }
        val result = KnownApiCaller(
            jacksonObjectMapper(),
            transport,
            PortalCredentialProvider { mapOf("Cookie" to listOf("fresh=session")) },
        ).call(scheduleKnowledge(), mapOf("date" to "2026-06-10"))

        assertTrue(result.success)
        assertEquals(KnownApiCallStatus.SUCCESS, result.status)
        assertEquals("https://portal.example/api/schedule?date=2026-06-10", requestedUrl)
        assertEquals(listOf("fresh=session"), requestedHeaders["Cookie"])
        assertEquals("2026-06-10T09:25", result.events.single().start)
    }

    @Test
    fun `KnownApiCaller sends only headers required by knowledge policy`() {
        var sentHeaders: Map<String, List<String>> = emptyMap()
        val result = KnownApiCaller(
            jacksonObjectMapper(),
            PortalHttpTransport { request ->
                sentHeaders = request.headers
                PortalHttpResponse(
                    200,
                    emptyMap(),
                    """{"body":[{"tenMonHoc":"Mobile","ngayBatDauHoc":"10/06/2026","tuGio":"09:25","denGio":"11:55"}]}""",
                )
            },
            PortalCredentialProvider {
                mapOf("Authorization" to listOf("Bearer token"), "Cookie" to listOf("session=secret"))
            },
        ).call(
            scheduleKnowledge().copy(
                headersPolicy = com.example.smartcalendar.portaldiscovery.knowledge.HeadersPolicy(
                    requiredCredentialHeaders = listOf("Authorization"),
                ),
            ),
            mapOf("date" to "2026-06-10"),
        )

        assertTrue(result.success)
        assertEquals(setOf("Authorization"), sentHeaders.keys)
    }

    @Test
    fun `KnownApiCaller rejects missing parameter before network call`() {
        var called = false
        val result = KnownApiCaller(
            jacksonObjectMapper(),
            PortalHttpTransport {
                called = true
                PortalHttpResponse(200, emptyMap(), "{}")
            },
        ).call(scheduleKnowledge())

        assertFalse(called)
        assertEquals(KnownApiCallStatus.MISSING_PARAMETER, result.status)
    }

    @Test
    fun `KnownApiCaller returns login required for unauthorized session`() {
        val result = KnownApiCaller(
            jacksonObjectMapper(),
            PortalHttpTransport { PortalHttpResponse(401, emptyMap(), "{}") },
        ).call(scheduleKnowledge(), mapOf("date" to "2026-06-10"))

        assertEquals(KnownApiCallStatus.LOGIN_REQUIRED, result.status)
    }

    @Test
    fun `runtime CLI maps bearer token to Authorization header`() {
        assertEquals(
            listOf("Bearer token-value"),
            runtimeCredentialHeaders(null, "token-value")["Authorization"],
        )
        assertEquals(
            listOf("Bearer token-value"),
            runtimeCredentialHeaders("Authorization=Bearer token-value", null)["Authorization"],
        )
    }

    @Test
    fun `agent router uses per-agent model settings`() = runBlocking {
        val json = jacksonObjectMapper()
        val models = mutableListOf<String>()
        val transport = LlmTransport { request ->
            models += json.readTree(request.body).path("model").asText()
            LlmHttpResponse(200, """{"choices":[{"message":{"content":"{}"}}]}""")
        }
        val router = AgentLlmRouter(
            PortalDiscoveryConfig(
                nineRouterDefaultCombo = "openrouter-combo",
                nineRouterCodeCombo = "opencode-combo",
                nineRouterPalamedesModel = "palamedes-model",
                nineRouterPercivalModel = "percival-model",
                nineRouterKayModel = "kay-model",
                nineRouterMorganModel = "morgan-model",
                fozaMerlinModel = "merlin-model",
                fozaMerlinFallbackModel = "merlin-fallback",
            ),
            OpenAiCompatibleLlmClient(json, transport),
        )

        router.palamedes("{}", "{}")
        router.percival("{}", "{}")
        router.kay("{}", "{}")
        router.morgan("{}", "{}")
        router.merlin("{}", "{}")

        assertEquals(
            listOf("palamedes-model", "percival-model", "kay-model", "morgan-model", "merlin-model"),
            models,
        )
    }

    private fun scheduleKnowledge() = ApiKnowledge(
        id = 1,
        toolName = "get_student_schedule",
        purpose = "Get schedule",
        category = EndpointCategory.SCHEDULE,
        method = "GET",
        portalUrl = "https://portal.example",
        urlTemplate = "https://portal.example/api/schedule?date={date}",
        authType = AuthType.SESSION_CREDENTIALS_AT_RUNTIME,
        requiredParams = listOf(ApiParameter("date")),
        responseMapping = CalendarMapping("tenMonHoc", "tuGio", "denGio", confidence = 0.95, verified = true, date = "ngayBatDauHoc"),
        safetyLevel = SafetyLevel.READ_ONLY,
        confidence = 0.95,
        status = KnowledgeStatus.VERIFIED,
        source = KnowledgeSource.MANUAL,
        lastVerifiedAt = "2026-06-11T00:00:00Z",
        createdAt = "2026-06-11T00:00:00Z",
        updatedAt = "2026-06-11T00:00:00Z",
    )
}
