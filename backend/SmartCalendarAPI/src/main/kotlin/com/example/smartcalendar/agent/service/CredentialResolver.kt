package com.example.smartcalendar.agent.service

import com.example.smartcalendar.agent.dto.ResolvedCredentials
import com.example.smartcalendar.repository.PortalCredentialRepository
import org.springframework.stereotype.Service

@Service
class CredentialResolver(
    private val repository: PortalCredentialRepository
) {
    fun resolve(
        userId: Int,
        requiredHeaders: List<String>,
        optionalHeaders: List<String> = emptyList()
    ): ResolvedCredentials {
        val available = loadCredentialHeaders(userId)
        val headers = linkedMapOf<String, String>()
        val missing = mutableListOf<String>()
        requiredHeaders.distinctBy(String::lowercase).forEach { name ->
            val value = available.findHeader(name)
            if (value == null) missing += name else headers[name] = value
        }
        optionalHeaders.distinctBy(String::lowercase).forEach { name ->
            available.findHeader(name)?.let { headers[name] = it }
        }
        return ResolvedCredentials(headers, missing)
    }

    private fun loadCredentialHeaders(userId: Int): Map<String, String> {
        val credential = repository.findFirstByUserIdOrderByLastCapturedAtDescIdDesc(userId) ?: return emptyMap()
        return buildMap {
            credential.authorization?.takeIf(String::isNotBlank)?.let { put("Authorization", it) }
            credential.cookie?.takeIf(String::isNotBlank)?.let { put("Cookie", it) }
            credential.csrfToken?.takeIf(String::isNotBlank)?.let { put("X-CSRF-Token", it) }
        }
    }

    private fun Map<String, String>.findHeader(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.takeIf(String::isNotBlank)
}
