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
import java.time.LocalDateTime

@Entity
@Table(name = "api_knowledge")
class ApiKnowledgeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(name = "discovery_job_id", length = 36)
    var discoveryJobId: String? = null,

    @Column(name = "natural_key", nullable = false, length = 64)
    var naturalKey: String = "",

    @Column(name = "tool_name", nullable = false, length = 255)
    var toolName: String = "",

    @Column(name = "portal_url", nullable = false, length = 1000)
    var portalUrl: String = "",

    @Column(nullable = false, length = 20)
    var method: String = "",

    @Column(nullable = false, length = 100)
    var category: String = "",

    @Column(name = "knowledge_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    var knowledgeJson: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
