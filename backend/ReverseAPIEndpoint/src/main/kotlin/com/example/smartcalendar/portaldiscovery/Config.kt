package com.example.smartcalendar.portaldiscovery

import java.nio.file.Files
import java.nio.file.Path

data class PortalDiscoveryConfig(
    val nineRouterUrl: String = "http://localhost:20128",
    val nineRouterKey: String? = null,
    val nineRouterPalamedesModel: String = "openrouter-combo",
    val nineRouterPercivalModel: String = "openrouter-combo",
    val nineRouterKayModel: String = "openrouter-combo",
    val nineRouterMorganModel: String = "opencode-combo",
    val fozaBaseUrl: String = "https://api.foza.ai/v1",
    val fozaApiKey: String? = null,
    val fozaMerlinModel: String = "hoang/claude-sonnet-4.6",
    val fozaMerlinFallbackModel: String = "hoang/gpt-5.5",
    val apiKnowledgeFile: String = ".portal-discovery/api-knowledge.json",
) {
    companion object {
        fun fromEnvironment(): PortalDiscoveryConfig {
            val env = loadDotenv()
            fun value(name: String, default: String? = null) = System.getProperty(name) ?: System.getenv(name) ?: env[name] ?: default
            return PortalDiscoveryConfig(
                nineRouterUrl = value("NINEROUTER_URL", "http://localhost:20128")!!,
                nineRouterKey = value("NINEROUTER_KEY"),
                nineRouterPalamedesModel = value("NINEROUTER_PALAMEDES_MODEL", "openrouter-combo")!!,
                nineRouterPercivalModel = value("NINEROUTER_PERCIVAL_MODEL", "openrouter-combo")!!,
                nineRouterKayModel = value("NINEROUTER_KAY_MODEL", "openrouter-combo")!!,
                nineRouterMorganModel = value("NINEROUTER_MORGAN_MODEL", "opencode-combo")!!,
                fozaBaseUrl = value("FOZA_BASE_URL", "https://api.foza.ai/v1")!!,
                fozaApiKey = value("FOZA_API_KEY"),
                fozaMerlinModel = value("FOZA_MERLIN_MODEL", "hoang/claude-sonnet-4.6")!!,
                fozaMerlinFallbackModel = value("FOZA_MERLIN_FALLBACK_MODEL", "hoang/gpt-5.5")!!,
                apiKnowledgeFile = value("API_KNOWLEDGE_FILE", ".portal-discovery/api-knowledge.json")!!,
            )
        }

        private fun loadDotenv(): Map<String, String> = Path.of(".env").takeIf(Files::isRegularFile)?.let { path ->
            Files.readAllLines(path).map(String::trim).filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim().trim('"', '\'') }
        }.orEmpty()
    }
}
