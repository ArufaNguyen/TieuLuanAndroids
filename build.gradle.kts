// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
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

val startMssqlDocker by tasks.registering {
    group = "smart calendar"
    description = "Starts the local MSSQL Docker container used by the backend."

    doLast {
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

    mustRunAfter(startMssqlDocker)

    doLast {
        val envFile = file("tunnel-url-publisher/.env")
        if (!envFile.exists()) {
            throw GradleException(
                "Missing tunnel-url-publisher/.env. Create it from .env.example and set GITHUB_TOKEN."
            )
        }

        val gradlew = file("gradlew.bat").absolutePath
        val process = ProcessBuilder(
            gradlew,
            ":tunnel-url-publisher:run"
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

        println("[smart-calendar] tunnel publisher started")
    }
}

gradle.projectsEvaluated {
    tasks.findByPath(":backend:bootRun")?.dependsOn(startMssqlDocker, startTunnelPublisher)
}
