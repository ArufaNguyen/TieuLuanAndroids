package com.example.smartcalendar.portaldiscovery.core

data class PortalHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, List<String>>,
    val body: String? = null,
)

data class PortalHttpResponse(
    val status: Int,
    val headers: Map<String, List<String>>,
    val body: String,
)

fun interface PortalHttpTransport {
    fun execute(request: PortalHttpRequest): PortalHttpResponse
}

fun interface PortalCredentialProvider {
    fun headersFor(url: String): Map<String, List<String>>

    companion object {
        val NONE = PortalCredentialProvider { emptyMap() }
    }
}
