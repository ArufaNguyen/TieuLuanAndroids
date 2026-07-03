// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

fun commandSucceeds(command: List<String>): Boolean {
    return try {
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
}

fun envValue(name: String): String? {
    return providers.environmentVariable(name).orNull
        ?: file("backend/SmartCalendarAPI/.env")
            .takeIf { it.exists() }
            ?.readLines()
            ?.firstOrNull { it.trim().startsWith("$name=") }
            ?.substringAfter("=")
            ?.trim()
}

fun hypervisorMode(): String {
    return envValue("HYPERVISOR")?.lowercase()?.ifBlank { "docker" } ?: "docker"
}

fun autoStartTunnel(): Boolean {
    return envValue("AUTO_START_TUNNEL")?.equals("true", ignoreCase = true)
        ?: true
}

val startMssqlDocker by tasks.registering {
    group = "smart calendar"
    description = "Starts the local MSSQL Docker container used by the backend."

    doLast {
        if (hypervisorMode() == "direct") {
            println("[smart-calendar] HYPERVISOR=direct; skipping Docker startup. Use local SQL Server from backend/SmartCalendarAPI/.env.")
            return@doLast
        }

        if (!commandSucceeds(listOf("docker", "--version"))) {
            throw GradleException("Docker CLI is not installed or not available in PATH.")
        }

        if (!commandSucceeds(listOf("docker", "info"))) {
            println("[smart-calendar] Docker daemon is not running. Starting Docker Desktop...")

            val dockerDesktop = file("C:/Program Files/Docker/Docker/Docker Desktop.exe")
            if (!dockerDesktop.exists()) {
                throw GradleException(
                    "Docker Desktop is not running, and Docker Desktop.exe was not found at ${dockerDesktop.absolutePath}."
                )
            }

            ProcessBuilder(dockerDesktop.absolutePath).start()

            val ready = (1..60).any {
                Thread.sleep(2_000)
                commandSucceeds(listOf("docker", "info"))
            }

            if (!ready) {
                throw GradleException("Docker Desktop did not become ready after 120 seconds. Open Docker Desktop manually, then run the command again.")
            }
        }

        println("[smart-calendar] starting MSSQL container TLANDROIDserver")
        val process = ProcessBuilder("docker", "start", "TLANDROIDserver")
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach(::println)
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("Failed to start Docker container TLANDROIDserver. Docker exit code: $exitCode")
        }
    }
}

val startTunnelPublisher by tasks.registering {
    group = "smart calendar"
    description = "Starts Cloudflare Tunnel publisher for the backend API."

    dependsOn(":tunnel-url-publisher:classes")
    mustRunAfter(startMssqlDocker)

    doLast {
        val tunnelEnvFile = file("tunnel-url-publisher/.env")
        if (!tunnelEnvFile.exists()) {
            throw GradleException(
                "Missing tunnel-url-publisher/.env. Create it from .env.example and set GITHUB_TOKEN."
            )
        }

        val publisherProject = project(":tunnel-url-publisher")
        val sourceSets = publisherProject.extensions
            .getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
        val runtimeClasspath = sourceSets.getByName("main").runtimeClasspath.asPath
        val javaExecutable = file(
            "${System.getProperty("java.home")}/bin/${if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"}"
        ).absolutePath

        val process = ProcessBuilder(
            javaExecutable,
            "-cp",
            runtimeClasspath,
            "MainKt"
        )
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()

        Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach(::println)
            }
        }.apply {
            isDaemon = true
            name = "tunnel-url-publisher-log"
            start()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            if (process.isAlive) {
                process.destroy()
            }
        })

        println("[smart-calendar] tunnel publisher scheduled; cloudflared will start after backend is ready")
    }
}

gradle.projectsEvaluated {
    tasks.findByPath(":backend:bootRun")?.dependsOn(startMssqlDocker)
    if (autoStartTunnel()) {
        tasks.findByPath(":backend:bootRun")?.dependsOn(startTunnelPublisher)
    } else {
        println("[smart-calendar] automatic tunnel startup disabled; set AUTO_START_TUNNEL=true to enable it.")
    }
}
