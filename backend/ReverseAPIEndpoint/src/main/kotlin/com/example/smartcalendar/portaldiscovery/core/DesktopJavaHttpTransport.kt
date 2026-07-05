package com.example.smartcalendar.portaldiscovery.core

import java.net.URI
import java.net.InetAddress
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DesktopJavaHttpTransport : PortalHttpTransport {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    override fun execute(request: PortalHttpRequest): PortalHttpResponse {
        val uri = URI(request.url)
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "Only HTTP and HTTPS portal URLs are allowed."
        }
        val host = requireNotNull(uri.host) { "Portal URL must contain a valid host." }
        require(InetAddress.getAllByName(host).none(::isPrivateAddress)) {
            "Replay to local or private network addresses is blocked."
        }
        val builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
        request.headers.forEach { (name, values) -> values.forEach { builder.header(name, it) } }
        val body = request.body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()
        val response = client.send(builder.method(request.method, body).build(), HttpResponse.BodyHandlers.ofString())
        return PortalHttpResponse(response.statusCode(), response.headers().map(), response.body())
    }

    private fun isPrivateAddress(address: InetAddress): Boolean =
        address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
}
