plugins {
    kotlin("jvm") version "1.9.25"
    application
}

group = "com.example.smartcalendar"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("com.example.smartcalendar.portaldiscovery.MainKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    from(sourceSets.main.get().output)
}

tasks.register<JavaExec>("runDesktopDiscovery") {
    group = "portal discovery"
    description = "Runs the manual full desktop discovery workflow against a HAR file."
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.smartcalendar.portaldiscovery.MainKt")
    providers.gradleProperty("harFile").orNull?.let { args(it) }
}

tasks.register<JavaExec>("runKnownTool") {
    group = "portal discovery"
    description = "Runs a previously persisted read-only API knowledge tool."
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.example.smartcalendar.portaldiscovery.RunKnownToolMainKt")
    val portalUrl = providers.gradleProperty("portalUrl").orNull ?: providers.environmentVariable("PORTAL_URL").orNull
    val toolName = providers.gradleProperty("toolName").orNull ?: providers.environmentVariable("PORTAL_TOOL_NAME").orNull
    val parameters = (providers.gradleProperty("toolParams").orNull ?: providers.environmentVariable("PORTAL_TOOL_PARAMS").orNull)
        ?.split(',')
        ?.filter(String::isNotBlank)
        .orEmpty()
    if (portalUrl != null && toolName != null) args(listOf(portalUrl, toolName) + parameters)
}
