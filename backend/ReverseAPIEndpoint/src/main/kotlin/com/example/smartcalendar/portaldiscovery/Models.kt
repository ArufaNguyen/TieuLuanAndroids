package com.example.smartcalendar.portaldiscovery

data class DesktopDiscoveryRequest(
    val harFilePath: String? = null,
    val harRawJson: String? = null,
    val capturedExchanges: List<CapturedExchange>? = null,
    val requireManualApprovalForPostReplay: Boolean = true,
    val manualApprovedEndpointIds: Set<String> = emptySet(),
)

data class DiscoveryRequest(
    val capturedExchanges: List<CapturedExchange>,
    val source: KnowledgeSource = KnowledgeSource.MANUAL,
    val requireManualApprovalForPostReplay: Boolean = true,
    val manualApprovedEndpointIds: Set<String> = emptySet(),
)

data class CapturedExchange(
    val method: String,
    val url: String,
    val requestHeaders: Map<String, List<String>> = emptyMap(),
    val requestBody: String? = null,
    val responseStatus: Int? = null,
    val responseContentType: String? = null,
    val responseBody: String? = null,
    val responseHeaders: Map<String, List<String>> = emptyMap(),
)

data class DiscoveryResult(
    val success: Boolean,
    val status: DiscoveryStatus,
    val primaryTool: ToolDefinition? = null,
    val verifiedEndpoints: List<VerifiedEndpoint> = emptyList(),
    val persistedKnowledgeIds: List<Int> = emptyList(),
    @Deprecated("Use primaryTool.endpointId or verifiedEndpoints.")
    val verifiedEndpointId: String? = null,
    val mapping: CalendarMapping? = null,
    @Deprecated("Use primaryTool.")
    val tool: ToolDefinition? = null,
    val approvedEndpoints: List<ApprovedEndpoint> = emptyList(),
    val tools: List<ToolDefinition> = emptyList(),
    val eventsPreview: List<CalendarEventDto> = emptyList(),
    val failureReason: String? = null,
    val warnings: List<String> = emptyList(),
    val trace: DiscoveryTrace = DiscoveryTrace(),
)

typealias DesktopDiscoveryResult = DiscoveryResult

enum class DiscoveryStatus { TOOL_CREATED, ENDPOINTS_APPROVED, FAILED_VALIDATION, NO_CANDIDATE, REPLAY_FAILED, SESSION_EXPIRED, MAPPING_FAILED }

enum class KnowledgeSource { HAR, ANDROID_WEBVIEW, MANUAL }

enum class EndpointCategory {
    LOGIN,
    SCHEDULE,
    REGISTERED_COURSES,
    AVAILABLE_COURSES,
    RETAKE_COURSES,
    NOTIFICATION,
    SEMESTER,
    DANGEROUS_WRITE,
    OTHER,
}

data class ApprovedEndpoint(
    val endpointId: String,
    val category: EndpointCategory,
    val confidence: Double,
    val replayVerified: Boolean,
    val replayFailureReason: String? = null,
)

data class VerifiedEndpoint(
    val endpointId: String,
    val category: EndpointCategory,
    val method: String,
    val urlTemplate: String,
    val confidence: Double,
    val requiredCredentialHeaders: Set<String> = emptySet(),
    val requiredStaticHeaders: Set<String> = emptySet(),
)

data class CalendarEventDto(
    val title: String,
    val start: String,
    val end: String,
    val location: String? = null,
    val description: String? = null,
    val source: String = "portal",
    val raw: Map<String, Any?> = emptyMap(),
)

data class CalendarMapping(
    val title: String,
    val start: String,
    val end: String,
    val location: String? = null,
    val description: String? = null,
    val confidence: Double,
    val verified: Boolean,
    val date: String? = null,
)

data class ToolDefinition(
    val toolName: String,
    val endpointId: String,
    val category: EndpointCategory,
    val mapping: CalendarMapping? = null,
)
data class DiscoveryTrace(val importedExchangeCount: Int = 0, val endpointCount: Int = 0, val candidates: List<CandidateTrace> = emptyList())
data class CandidateTrace(
    val endpointId: String,
    val method: String,
    val url: String,
    val category: EndpointCategory = EndpointCategory.OTHER,
    val blocked: Boolean,
    val heuristicConfidence: Double,
    val percivalConfidence: Double? = null,
    val merlinVerdict: String? = null,
    val arthurDecision: String,
)
