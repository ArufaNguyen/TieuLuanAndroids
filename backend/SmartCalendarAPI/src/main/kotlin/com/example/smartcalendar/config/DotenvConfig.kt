package com.example.smartcalendar.config

import java.nio.file.Files
import java.nio.file.Path

object DotenvConfig {

    fun load() {
        listOf(
            Path.of(".env"),
            Path.of("backend", "SmartCalendarAPI", ".env")
        ).forEach(::loadFile)
    }

    private fun loadFile(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.readAllLines(path)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val key = line.substringBefore("=").trim()
                val value = line.substringAfter("=").trim()

                if (System.getProperty(key).isNullOrBlank() && System.getenv(key).isNullOrBlank()) {
                    System.setProperty(key, value)
                }
            }
    }
}
