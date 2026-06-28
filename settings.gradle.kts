pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TieuLuanAndroids"
include(":app")

val backendDir = file("backend/SmartCalendarAPI")
if (backendDir.exists()) {
    include(":backend")
    project(":backend").projectDir = backendDir
}

val tunnelPublisherDir = file("tunnel-url-publisher")
if (tunnelPublisherDir.exists()) {
    include(":tunnel-url-publisher")
    project(":tunnel-url-publisher").projectDir = tunnelPublisherDir
}
