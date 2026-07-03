package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.DiscoveryRequest
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.SafetyResult

class MordredReplayGatekeeper {
    fun canReplay(endpoint: Endpoint, safety: SafetyResult, request: DiscoveryRequest): Boolean {
        if (safety.blocked || !safety.allowed) return false
        return endpoint.method != "POST" ||
            !request.requireManualApprovalForPostReplay ||
            endpoint.id in request.manualApprovedEndpointIds
    }
}
