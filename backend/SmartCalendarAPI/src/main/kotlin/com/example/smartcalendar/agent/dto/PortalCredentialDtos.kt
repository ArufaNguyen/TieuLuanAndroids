package com.example.smartcalendar.agent.dto

import java.time.Instant

data class StartPortalCredentialCaptureRequest(
    val loginToolId: Int? = null
)

data class PortalCredentialCaptureResponse(
    val captureId: String,
    val loginToolId: Int?,
    val portalUrl: String?,
    val loginUrlTemplate: String?,
    val requiredHeaders: List<String>,
    val hasAuthorization: Boolean,
    val hasCookie: Boolean,
    val hasCsrfToken: Boolean,
    val expiresAt: Instant?,
    val lastCapturedAt: Instant?
)

data class CompletePortalCredentialCaptureRequest(
    val authorization: String? = null,
    val cookie: String? = null,
    val csrfToken: String? = null,
    val expiresAt: Instant? = null
)

