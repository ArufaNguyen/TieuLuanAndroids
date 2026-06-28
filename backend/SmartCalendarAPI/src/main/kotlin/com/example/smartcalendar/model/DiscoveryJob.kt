package com.example.smartcalendar.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "discovery_jobs")
class DiscoveryJob(
    @Id
    @Column(length = 36)
    var id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: DiscoveryJobStatus = DiscoveryJobStatus.PROCESSING,

    @Column(name = "file_name", length = 255)
    var fileName: String? = null,

    @Column(name = "result_json", columnDefinition = "NVARCHAR(MAX)")
    var resultJson: String? = null,

    @Column(name = "error_message", columnDefinition = "NVARCHAR(MAX)")
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
)

enum class DiscoveryJobStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
