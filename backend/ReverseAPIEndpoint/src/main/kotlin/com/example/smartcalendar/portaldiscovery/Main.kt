package com.example.smartcalendar.portaldiscovery

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val har = args.firstOrNull() ?: error("Usage: runDesktopDiscovery <file.har>")
    println(
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
            runDesktopDiscovery(DesktopDiscoveryRequest(harFilePath = har)),
        ),
    )
}
