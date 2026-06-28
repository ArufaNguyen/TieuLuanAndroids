package com.example.smartcalendar.portaldiscovery.core

import com.example.smartcalendar.portaldiscovery.CapturedExchange
import java.net.URI

class EndpointCollector {
    fun collect(exchanges: List<CapturedExchange>): List<Endpoint> =
        exchanges.filterNot(::isNoise)
            .groupBy { "${it.method.uppercase()} ${normalizeUrl(it.url)}" }
            .map { (id, samples) ->
                val sample = bestSample(samples)
                Endpoint(id, sample.method.uppercase(), normalizeUrl(sample.url), sample)
            }

    private fun isNoise(exchange: CapturedExchange): Boolean {
        val url = exchange.url.lowercase()
        val path = url.substringBefore('?')
        return STATIC.any(path::endsWith) || NOISE_HOSTS.any(url::contains) || url.contains("/recaptcha/")
    }

    private fun normalizeUrl(url: String): String = runCatching {
        val uri = URI(url)
        "${uri.scheme}://${uri.authority}${uri.path}" + uri.query?.split('&')?.sorted()?.mapNotNull { part ->
            val key = part.substringBefore('=')
            if (key.lowercase() in CACHE_PARAMS) null else "$key={$key}"
        }?.joinToString("&", "?").orEmpty()
    }.getOrDefault(url)

    private fun bestSample(samples: List<CapturedExchange>): CapturedExchange =
        samples.maxByOrNull(::sampleScore) ?: samples.first()

    private fun sampleScore(exchange: CapturedExchange): Int {
        val body = exchange.responseBody.orEmpty()
        return (if (exchange.responseStatus in 200..299) 100 else 0) +
            (if (body.isNotBlank()) 50 else 0) +
            (if (body.contains("\"body\"", ignoreCase = true)) 10 else 0)
    }

    companion object {
        val STATIC = setOf(".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".woff", ".woff2", ".ttf", ".map")
        val CACHE_PARAMS = setOf("_", "_t", "timestamp", "cachebust", "random")
        val NOISE_HOSTS = setOf("google-analytics.com", "googletagmanager.com", "doubleclick.net", "fonts.googleapis.com")
    }
}
