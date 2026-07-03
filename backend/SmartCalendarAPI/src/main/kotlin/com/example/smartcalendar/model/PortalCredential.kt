package com.example.smartcalendar.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "portal_credentials")
class PortalCredential(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @Column(name = "capture_id", nullable = false, unique = true, length = 36)
    var captureId: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(name = "login_tool_id")
    var loginToolId: Int? = null,

    @Column(name = "portal_url", length = 1000)
    var portalUrl: String? = null,

    @Column(name = "login_url_template", length = 2000)
    var loginUrlTemplate: String? = null,

    @Column(name = "authorization_header", columnDefinition = "NVARCHAR(MAX)")
    var authorization: String? = null,

    @Column(name = "cookie", columnDefinition = "NVARCHAR(MAX)")
    var cookie: String? = null,

    @Column(name = "csrf_token", columnDefinition = "NVARCHAR(MAX)")
    var csrfToken: String? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "last_captured_at")
    var lastCapturedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
