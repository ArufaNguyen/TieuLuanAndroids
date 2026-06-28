package com.example.smartcalendar.portaldiscovery.runtime

import com.example.smartcalendar.portaldiscovery.PortalDiscoveryConfig
import com.example.smartcalendar.portaldiscovery.core.DesktopJavaHttpTransport
import com.example.smartcalendar.portaldiscovery.core.PortalCredentialProvider
import com.example.smartcalendar.portaldiscovery.core.PortalHttpTransport
import com.example.smartcalendar.portaldiscovery.knowledge.findApiKnowledge
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun runKnownTool(
    portalUrl: String,
    toolName: String,
    parameters: Map<String, Any?> = emptyMap(),
    credentialProvider: PortalCredentialProvider,
    transport: PortalHttpTransport = DesktopJavaHttpTransport(),
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): KnownApiCallResult {
    val knowledge = findApiKnowledge(portalUrl, toolName, config)
        ?: return KnownApiCallResult(
            false,
            KnownApiCallStatus.INVALID_KNOWLEDGE,
            failureReason = "No verified knowledge found for $toolName at $portalUrl.",
        )
    return KnownApiCaller(jacksonObjectMapper(), transport, credentialProvider).call(knowledge, parameters)
}
