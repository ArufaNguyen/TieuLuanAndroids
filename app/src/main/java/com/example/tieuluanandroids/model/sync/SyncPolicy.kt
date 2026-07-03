package com.example.tieuluanandroids.model.sync

import com.example.tieuluanandroids.model.*
import com.example.tieuluanandroids.model.local.*
import com.example.tieuluanandroids.model.service.*

object SyncPolicy {
    const val UNIQUE_WORK_NAME = "smart-calendar-sync"
    const val ENTITY_EVENT = "EVENT"
    const val ENTITY_TAG = "TAG"
    const val OPERATION_CREATE = "CREATE"
    const val OPERATION_UPDATE = "UPDATE"
    const val OPERATION_DELETE = "DELETE"

    // Simplified LWW: pending local writes win until pushed; server wins for SYNCED rows on pull.
    const val CONFLICT_POLICY = "LAST_WRITE_WINS"
}
