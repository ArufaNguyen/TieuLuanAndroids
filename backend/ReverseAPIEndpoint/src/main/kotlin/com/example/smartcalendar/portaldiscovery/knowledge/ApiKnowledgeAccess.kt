package com.example.smartcalendar.portaldiscovery.knowledge

import com.example.smartcalendar.portaldiscovery.PortalDiscoveryConfig
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Path

fun loadApiKnowledge(
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): List<ApiKnowledge> = JsonFileApiKnowledgeRepository(
    Path.of(config.apiKnowledgeFile),
    jacksonObjectMapper(),
).findAll()

fun findApiKnowledge(
    toolName: String,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): ApiKnowledge? = JsonFileApiKnowledgeRepository(
    Path.of(config.apiKnowledgeFile),
    jacksonObjectMapper(),
).findByToolName(toolName)

fun loadApiKnowledgeForPortal(
    portalUrl: String,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): List<ApiKnowledge> = JsonFileApiKnowledgeRepository(
    Path.of(config.apiKnowledgeFile),
    jacksonObjectMapper(),
).findByPortalUrl(portalUrl)

fun findApiKnowledge(
    portalUrl: String,
    toolName: String,
    config: PortalDiscoveryConfig = PortalDiscoveryConfig.fromEnvironment(),
): ApiKnowledge? = JsonFileApiKnowledgeRepository(
    Path.of(config.apiKnowledgeFile),
    jacksonObjectMapper(),
).findByPortalAndTool(portalUrl, toolName)
