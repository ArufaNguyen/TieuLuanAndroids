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

rootProject.name = "TieuLuanAndroidsBackend"

val backendDir = file("backend/SmartCalendarAPI")
if (backendDir.exists()) {
    include(":backend")
    project(":backend").projectDir = backendDir
}

val reverseApiEndpointDir = file("backend/ReverseAPIEndpoint")
if (reverseApiEndpointDir.exists()) {
    include(":reverse-api-endpoint")
    project(":reverse-api-endpoint").projectDir = reverseApiEndpointDir
}

val tunnelPublisherDir = file("tunnel-url-publisher")
if (tunnelPublisherDir.exists()) {
    include(":tunnel-url-publisher")
    project(":tunnel-url-publisher").projectDir = tunnelPublisherDir
}
