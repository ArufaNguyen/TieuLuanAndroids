package com.example.tieuluanandroids.data.model

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNC_FAILED,
    CONFLICT
}
