package com.example.smartcalendar.portaldiscovery

import com.example.smartcalendar.portaldiscovery.core.PortalCredentialProvider
import com.example.smartcalendar.portaldiscovery.runtime.runKnownTool
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun main(args: Array<String>) {
    val portalUrl = args.getOrNull(0) ?: error("Usage: runKnownTool <portalUrl> <toolName> [name=value ...]")
    val toolName = args.getOrNull(1) ?: error("Usage: runKnownTool <portalUrl> <toolName> [name=value ...]")
    val parameters = args.drop(2).associate { argument ->
        require(argument.contains('=')) { "Tool parameters must use name=value." }
        argument.substringBefore('=') to argument.substringAfter('=')
    }
    val runtimeHeaders = runtimeCredentialHeaders(
        System.getenv("PORTAL_COOKIE"),
        System.getenv("PORTAL_AUTHORIZATION"),
    )
    val credentials = PortalCredentialProvider { runtimeHeaders }
    val result = runKnownTool(portalUrl, toolName, parameters, credentials)
    println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result))
}

fun runtimeCredentialHeaders(cookieValue: String?, authorizationValue: String?): Map<String, List<String>> {
    val headers = mutableMapOf<String, List<String>>()
    val cookie = cookieValue.orEmpty().trim()
    val authorization = authorizationValue.orEmpty().trim()
    when {
        authorization.isNotBlank() -> headers["Authorization"] = listOf(normalizeAuthorization(authorization))
        cookie.startsWith("Authorization=", ignoreCase = true) ->
            headers["Authorization"] = listOf(normalizeAuthorization(cookie.substringAfter('=')))
        cookie.isNotBlank() -> headers["Cookie"] = listOf(cookie)
    }
    return headers
}

private fun normalizeAuthorization(value: String): String =
    value.removePrefix("Authorization=").trim().let { if (it.startsWith("Bearer ", true)) it else "Bearer $it" }
