package com.example.smartcalendar.portaldiscovery.agents

import com.example.smartcalendar.portaldiscovery.CapturedExchange
import com.example.smartcalendar.portaldiscovery.core.Endpoint
import com.example.smartcalendar.portaldiscovery.core.EndpointCollector

class GalahadCollectorAgent(private val collector: EndpointCollector = EndpointCollector()) {
    fun collect(exchanges: List<CapturedExchange>): List<Endpoint> = collector.collect(exchanges)
}
