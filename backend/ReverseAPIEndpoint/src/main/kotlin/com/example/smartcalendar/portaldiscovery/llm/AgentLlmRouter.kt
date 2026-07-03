package com.example.smartcalendar.portaldiscovery.llm

import com.example.smartcalendar.portaldiscovery.PortalDiscoveryConfig

class AgentLlmRouter(private val config: PortalDiscoveryConfig, private val client: OpenAiCompatibleLlmClient) {
    suspend fun palamedes(schema: String, input: String) = nineRouterAgent(
        "Palamedes",
        config.nineRouterPalamedesModel,
        schema,
        input,
    )

    suspend fun percival(schema: String, input: String) = nineRouterAgent(
        "Percival",
        config.nineRouterPercivalModel,
        schema,
        input,
    )

    suspend fun kay(schema: String, input: String) = nineRouterAgent(
        "Kay",
        config.nineRouterKayModel,
        schema,
        input,
    )

    suspend fun morgan(schema: String, input: String) = nineRouterAgent(
        "MorganVulnerabilityDetector",
        config.nineRouterMorganModel,
        schema,
        input,
    )

    suspend fun merlin(schema: String, input: String) = client.complete(
        config.fozaBaseUrl, config.fozaApiKey,
        listOf(config.fozaMerlinModel, config.fozaMerlinFallbackModel),
        "Merlin", schema, input,
    )

    private suspend fun nineRouterAgent(agent: String, model: String, schema: String, input: String) = client.complete(
        config.nineRouterUrl, config.nineRouterKey,
        listOf(model),
        agent, schema, input,
    )
}
