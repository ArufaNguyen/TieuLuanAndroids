package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CapturedExchange
import com.example.smartcalendar.portaldiscovery.DesktopDiscoveryRequest
import com.fasterxml.jackson.databind.ObjectMapper

@Deprecated("Use HarCaptureAdapter or pass CapturedExchange directly to runDiscovery.")
class CaptureImporter(private val json: ObjectMapper) {
    private val har = HarCaptureAdapter(json)

    fun import(request: DesktopDiscoveryRequest): List<CapturedExchange> {
        val provided = listOf(request.harFilePath, request.harRawJson, request.capturedExchanges).count {
            when (it) { is String -> it.isNotBlank(); is List<*> -> true; else -> false }
        }
        require(provided == 1) { "Provide exactly one capture source." }
        request.capturedExchanges?.let { return it }
        return request.harRawJson?.let(har::fromRawJson) ?: har.fromFile(requireNotNull(request.harFilePath))
    }
}
