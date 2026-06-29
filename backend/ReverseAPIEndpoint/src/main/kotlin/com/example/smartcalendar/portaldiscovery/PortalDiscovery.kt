package com.example.smartcalendar.portaldiscovery

import com.example.smartcalendar.portaldiscovery.agents.ArthurJudgeAgent
import com.example.smartcalendar.portaldiscovery.agents.BedivereToolDesignerAgent
import com.example.smartcalendar.portaldiscovery.agents.GalahadCollectorAgent
import com.example.smartcalendar.portaldiscovery.agents.GawainSafetyAgent
import com.example.smartcalendar.portaldiscovery.agents.KayCalendarMapperAgent
import com.example.smartcalendar.portaldiscovery.agents.MerlinAdversarialVerifierAgent
import com.example.smartcalendar.portaldiscovery.agents.MordredReplayGatekeeper
import com.example.smartcalendar.portaldiscovery.agents.MorganVulnerabilityDetectorAgent
import com.example.smartcalendar.portaldiscovery.agents.PalamedesClassifierAgent
import com.example.smartcalendar.portaldiscovery.agents.PercivalReadEndpointHunterAgent
import com.example.smartcalendar.portaldiscovery.agents.ScheduleSignalAgent
import com.example.smartcalendar.portaldiscovery.core.CalendarNormalizer
import com.example.smartcalendar.portaldiscovery.core.DesktopJavaHttpTransport
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.HarCaptureAdapter
import com.example.smartcalendar.portaldiscovery.core.HeaderRequirementVerifier
import com.example.smartcalendar.portaldiscovery.core.PortalCredentialProvider
import com.example.smartcalendar.portaldiscovery.core.PortalHttpTransport
import com.example.smartcalendar.portaldiscovery.core.ReplayEngine
import com.example.smartcalendar.portaldiscovery.llm.AgentLlmRouter
import com.example.smartcalendar.portaldiscovery.llm.DesktopJavaLlmTransport
import com.example.smartcalendar.portaldiscovery.llm.LlmTransport
import com.example.smartcalendar.portaldiscovery.llm.OpenAiCompatibleLlmClient
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledgeFactory
import com.example.smartcalendar.portaldiscovery.knowledge.ApiKnowledgeRepository
import com.example.smartcalendar.portaldiscovery.knowledge.JsonFileApiKnowledgeRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Path

suspend fun runDesktopDiscovery(
    request: DesktopDiscoveryRequest,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
    portalHttpTransport: PortalHttpTransport = DesktopJavaHttpTransport(),
    credentialProvider: PortalCredentialProvider = PortalCredentialProvider.NONE,
    llmTransport: LlmTransport = DesktopJavaLlmTransport(),
    knowledgeRepository: ApiKnowledgeRepository? = null,
): DesktopDiscoveryResult {
    val json = jacksonObjectMapper()
    val sources = listOf(request.harFilePath, request.harRawJson, request.capturedExchanges).count {
        when (it) { is String -> it.isNotBlank(); is List<*> -> true; else -> false }
    }
    if (sources != 1) return invalidResult("Provide exactly one capture source.")
    val exchanges = runCatching {
        request.capturedExchanges
            ?: request.harRawJson?.let(HarCaptureAdapter(json)::fromRawJson)
            ?: HarCaptureAdapter(json).fromFile(requireNotNull(request.harFilePath))
    }.getOrElse { return invalidResult(it.message ?: "Invalid capture input.") }
    return runDiscovery(
        DiscoveryRequest(
            capturedExchanges = exchanges,
            source = if (request.capturedExchanges == null) KnowledgeSource.HAR else KnowledgeSource.MANUAL,
            requireManualApprovalForPostReplay = request.requireManualApprovalForPostReplay,
            manualApprovedEndpointIds = request.manualApprovedEndpointIds,
        ),
        config,
        portalHttpTransport,
        credentialProvider,
        llmTransport,
        knowledgeRepository,
    )
}

suspend fun runDiscovery(
    request: DiscoveryRequest,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
    portalHttpTransport: PortalHttpTransport = DesktopJavaHttpTransport(),
    credentialProvider: PortalCredentialProvider = PortalCredentialProvider.NONE,
    llmTransport: LlmTransport = DesktopJavaLlmTransport(),
    knowledgeRepository: ApiKnowledgeRepository? = null,
): DiscoveryResult = PortalDiscoveryFunction(config, portalHttpTransport, credentialProvider, llmTransport, knowledgeRepository).run(request)

class PortalDiscoveryFunction(
    config: PortalDiscoveryConfig,
    portalHttpTransport: PortalHttpTransport = DesktopJavaHttpTransport(),
    credentialProvider: PortalCredentialProvider = PortalCredentialProvider.NONE,
    llmTransport: LlmTransport = DesktopJavaLlmTransport(),
    knowledgeRepository: ApiKnowledgeRepository? = null,
) {
    private val json = jacksonObjectMapper()
    private val knowledgeRepository = knowledgeRepository
        ?: JsonFileApiKnowledgeRepository(Path.of(config.apiKnowledgeFile), json)
    private val knowledgeFactory = ApiKnowledgeFactory()
    private val galahad = GalahadCollectorAgent()
    private val gawain = GawainSafetyAgent()
    private val signals = ScheduleSignalAgent()
    private val router = AgentLlmRouter(config, OpenAiCompatibleLlmClient(json, llmTransport))
    private val palamedes = PalamedesClassifierAgent(router, json)
    private val percival = PercivalReadEndpointHunterAgent(router, json)
    private val merlin = MerlinAdversarialVerifierAgent(router, json)
    private val arthur = ArthurJudgeAgent()
    private val mordred = MordredReplayGatekeeper()
    private val replayEngine = ReplayEngine(json, portalHttpTransport, credentialProvider)
    private val morgan = MorganVulnerabilityDetectorAgent(router, json)
    private val headerVerifier = HeaderRequirementVerifier(replayEngine, morgan)
    private val kay = KayCalendarMapperAgent(router, json)
    private val normalizer = CalendarNormalizer(json)
    private val bedivere = BedivereToolDesignerAgent()

    suspend fun run(request: DiscoveryRequest): DiscoveryResult {
        val exchanges = request.capturedExchanges
        val endpoints = galahad.collect(exchanges)
        val traces = mutableListOf<CandidateTrace>()
        val approved = mutableListOf<ApprovedCandidate>()

        endpoints.forEach { endpoint ->
            val safety = gawain.review(endpoint)
            val signal = signals.detect(endpoint)
            if (endpoint.isManualApprovedLoginPost(request)) {
                traces += endpoint.trace(
                    false,
                    signal.confidence,
                    EndpointCategory.LOGIN,
                    1.0,
                    "MANUAL_APPROVED",
                    decision = "APPROVE",
                )
                approved += ApprovedCandidate(endpoint, EndpointCategory.LOGIN, 1.0)
                return@forEach
            }
            if (safety.blocked || !signal.candidate) {
                traces += endpoint.trace(
                    safety.blocked,
                    signal.confidence,
                    if (safety.blocked) EndpointCategory.DANGEROUS_WRITE else signal.suggestedCategory,
                    decision = if (safety.blocked) "BLOCKED" else "SKIPPED",
                )
                return@forEach
            }
            val classification = runCatching { palamedes.classify(endpoint) }.getOrNull()
            val candidate = classification?.let { runCatching { percival.hunt(endpoint, signal, it) }.getOrNull() }
            if (candidate == null) {
                val fallback = endpoint.heuristicReadCandidate(signal)
                if (fallback != null) {
                    traces += endpoint.trace(
                        false,
                        signal.confidence,
                        fallback.category,
                        fallback.confidence,
                        "HEURISTIC_PASS",
                        decision = "APPROVE",
                    )
                    approved += ApprovedCandidate(endpoint, fallback.category, fallback.confidence)
                    return@forEach
                }
                traces += endpoint.trace(false, signal.confidence, signal.suggestedCategory, decision = "LLM_FAILED")
                return@forEach
            }
            if (!candidate.isCandidate) {
                traces += endpoint.trace(false, signal.confidence, candidate.category, candidate.confidence, decision = "REJECT")
                return@forEach
            }
            val verdict = runCatching { merlin.verifyCandidate(endpoint, candidate) }.getOrNull()
            if (verdict == null) {
                traces += endpoint.trace(false, signal.confidence, candidate.category, candidate.confidence, decision = "VERIFY_FAILED")
                return@forEach
            }
            val decision = arthur.decideCandidate(safety, candidate, verdict)
            traces += endpoint.trace(false, signal.confidence, candidate.category, candidate.confidence, verdict, decision)
            if (decision == "APPROVE") approved += ApprovedCandidate(endpoint, candidate.category, candidate.confidence)
        }

        val trace = DiscoveryTrace(exchanges.size, endpoints.size, traces)
        val winners = approved.groupBy { it.category }.mapNotNull { (_, values) ->
            values.maxByOrNull { it.selectionScore() }
        }
        if (winners.isEmpty()) return failure(DiscoveryStatus.NO_CANDIDATE, "No supported read endpoint passed Arthur's decision.", trace)

        val approvedEndpoints = mutableListOf<ApprovedEndpoint>()
        val verifiedEndpoints = mutableListOf<VerifiedEndpoint>()
        val tools = mutableListOf<ToolDefinition>()
        val warnings = mutableListOf<String>()
        var scheduleMapping: CalendarMapping? = null
        var events: List<CalendarEventDto> = emptyList()
        var sessionExpired = false
        winners.forEach { winner ->
            val safety = gawain.review(winner.endpoint)
            if (winner.category != EndpointCategory.LOGIN && !mordred.canReplay(winner.endpoint, safety, request)) {
                approvedEndpoints += ApprovedEndpoint(winner.endpoint.id, winner.category, winner.confidence, false, "Mordred blocked replay.")
                return@forEach
            }
            val headerVerification = headerVerifier.verify(winner.endpoint.sample)
            val replay = headerVerification.replay
            approvedEndpoints += ApprovedEndpoint(winner.endpoint.id, winner.category, winner.confidence, replay.success, replay.reason)
            if (!replay.success) {
                sessionExpired = sessionExpired || replay.sessionExpired
                return@forEach
            }
            verifiedEndpoints += VerifiedEndpoint(
                winner.endpoint.id,
                winner.category,
                winner.endpoint.method,
                winner.endpoint.url,
                winner.confidence,
                headerVerification.requiredCredentialHeaders,
                headerVerification.requiredStaticHeaders,
            )
            if (winner.category != EndpointCategory.SCHEDULE) {
                tools += bedivere.design(winner.endpoint.id, winner.category)
                return@forEach
            }
            val proposed = runCatching { kay.map(replay.body.orEmpty()) }.getOrElse {
                warnings += "Kay mapping failed for ${winner.endpoint.id}: ${it.message}"
                tools += bedivere.design(winner.endpoint.id, winner.category)
                return@forEach
            }
            val normalized = normalizer.normalize(replay.body.orEmpty(), proposed)
            if (normalized.isEmpty()) {
                warnings += "Mapping for ${winner.endpoint.id} produced no calendar events."
                tools += bedivere.design(winner.endpoint.id, winner.category)
                return@forEach
            }
            val mappingVerdict = runCatching { merlin.verifyMapping(proposed, normalized.size) }.getOrElse {
                warnings += "Merlin mapping verification failed for ${winner.endpoint.id}: ${it.message}"
                tools += bedivere.design(winner.endpoint.id, winner.category)
                return@forEach
            }
            if (arthur.approveMapping(proposed, normalized.size, mappingVerdict)) {
                scheduleMapping = proposed.copy(verified = true)
                events = normalized
                tools += bedivere.design(winner.endpoint.id, winner.category, scheduleMapping)
            } else {
                warnings += "Arthur rejected mapping for ${winner.endpoint.id}: Merlin=$mappingVerdict, confidence=${proposed.confidence}, events=${normalized.size}."
                tools += bedivere.design(winner.endpoint.id, winner.category)
            }
        }

        val primaryTool = tools.firstOrNull { it.category == EndpointCategory.SCHEDULE } ?: tools.firstOrNull()
        val status = when {
            tools.isNotEmpty() -> DiscoveryStatus.TOOL_CREATED
            sessionExpired -> DiscoveryStatus.SESSION_EXPIRED
            approvedEndpoints.isNotEmpty() -> DiscoveryStatus.ENDPOINTS_APPROVED
            else -> DiscoveryStatus.REPLAY_FAILED
        }
        val knowledge = knowledgeFactory.create(tools, verifiedEndpoints, request)
        val persistedKnowledgeIds = runCatching {
            knowledgeRepository.saveAll(knowledge).map { it.id }.sorted()
        }.getOrElse {
            warnings += "API Knowledge persistence failed: ${it.message}"
            emptyList()
        }
        if (knowledge.isNotEmpty() && persistedKnowledgeIds.isEmpty() && warnings.none { it.startsWith("API Knowledge persistence failed") }) {
            warnings += "API Knowledge persistence is disabled."
        }
        return DiscoveryResult(
            success = tools.isNotEmpty(),
            status = status,
            primaryTool = primaryTool,
            verifiedEndpoints = verifiedEndpoints,
            persistedKnowledgeIds = persistedKnowledgeIds,
            verifiedEndpointId = primaryTool?.endpointId ?: verifiedEndpoints.firstOrNull()?.endpointId,
            mapping = scheduleMapping,
            tool = primaryTool,
            approvedEndpoints = approvedEndpoints,
            tools = tools,
            eventsPreview = events.take(10),
            failureReason = if (tools.isEmpty()) approvedEndpoints.firstNotNullOfOrNull(ApprovedEndpoint::replayFailureReason) else null,
            warnings = warnings,
            trace = trace,
        )
    }

    private fun failure(status: DiscoveryStatus, reason: String, trace: DiscoveryTrace = DiscoveryTrace()) =
        DiscoveryResult(false, status, failureReason = reason, trace = trace)

    private data class ApprovedCandidate(val endpoint: Endpoint, val category: EndpointCategory, val confidence: Double)

    private data class HeuristicCandidate(val category: EndpointCategory, val confidence: Double)

    private fun Endpoint.isManualApprovedLoginPost(request: DiscoveryRequest): Boolean {
        if (!method.equals("POST", true)) return false
        if (!url.contains("login", ignoreCase = true)) return false
        return request.manualApprovedEndpointIds.any { approved ->
            val normalized = approved.trim()
            normalized.equals(id, ignoreCase = true) ||
                normalized.equals("login", ignoreCase = true) ||
                normalized.equals("POST login", ignoreCase = true) ||
                normalized.equals("POST /login", ignoreCase = true) ||
                id.contains(normalized, ignoreCase = true)
        }
    }

    private fun Endpoint.heuristicReadCandidate(signal: com.example.smartcalendar.portaldiscovery.core.SignalResult): HeuristicCandidate? {
        if (!method.equals("GET", true)) return null
        if (signal.suggestedCategory !in HEURISTIC_READ_CATEGORIES) return null
        if (signal.confidence < 0.55) return null
        val body = sample.responseBody?.takeIf(String::isNotBlank) ?: return null
        if (runCatching { json.readTree(body) }.isFailure) return null
        return HeuristicCandidate(signal.suggestedCategory, signal.confidence.coerceAtLeast(0.85))
    }

    private fun ApprovedCandidate.selectionScore(): Double {
        if (category != EndpointCategory.SCHEDULE) return confidence
        val url = endpoint.url.lowercase()
        val sample = endpoint.sample.responseBody.orEmpty().lowercase()
        val hasDate = listOf("ngaybatdauhoc", "\"date\"", "\"ngay\"").any(sample::contains)
        val hasStart = listOf("tugio", "starttime", "start_time").any(sample::contains)
        val hasEnd = listOf("dengio", "endtime", "end_time").any(sample::contains)
        val detailEndpointBoost = when {
            "lichtuan" in url -> 2.0
            "thang" in url && "date=" in url -> 1.5
            "songayhoc" in url -> -1.0
            url.endsWith("/calendar") -> -0.5
            else -> 0.0
        }
        return confidence + detailEndpointBoost + if (hasDate && hasStart && hasEnd) 1.0 else 0.0
    }

    private companion object {
        val HEURISTIC_READ_CATEGORIES = setOf(
            EndpointCategory.SCHEDULE,
            EndpointCategory.REGISTERED_COURSES,
            EndpointCategory.AVAILABLE_COURSES,
            EndpointCategory.RETAKE_COURSES,
            EndpointCategory.NOTIFICATION,
            EndpointCategory.SEMESTER,
        )
    }
}

private fun invalidResult(reason: String) =
    DiscoveryResult(false, DiscoveryStatus.FAILED_VALIDATION, failureReason = reason)
