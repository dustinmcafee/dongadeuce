plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    application
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

application {
    mainClass.set("com.dustinmcafee.dongadeuce.server.MainKt")
}

dependencies {
    // Pre-built shared module JAR (Kotlin 1.9.21, binary compatible with 2.x)
    implementation(files("../shared/build/libs/shared-jvm-6.0.7-beta.jar"))

    // Ktor server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-network-tls-certificates:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Ktor client (needed at runtime for shared module's HttpEngine)
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Compose runtime (needed at classload time by shared module)
    runtimeOnly("org.jetbrains.compose.runtime:runtime-desktop:1.5.11")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("io.ktor:ktor-client-websockets:2.3.7")
}

kotlin {
    jvmToolchain(17)
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
}
