import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":shared"))
    // Include all OS dependencies for cross-platform compatibility
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.macos_arm64)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Ktor client for Scryfall API
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Image loading - TODO: Find a desktop-compatible solution
    // Kamel or other libraries for loading images on desktop

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.dustinmcafee.dongadeuce.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Commander MTG"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "Commander MTG"
                upgradeUuid = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d"
            }
        }
    }
}

// Task to create a cross-platform JAR for Windows
tasks.register<Copy>("packageWindowsJar") {
    dependsOn("packageUberJarForCurrentOS")
    from("build/compose/jars") {
        include("*.jar")
    }
    into("build/windows")
    rename { "dongadeuce-windows-${project.version}.jar" }

    doLast {
        println("Windows JAR created at: ${layout.buildDirectory.get()}/windows/dongadeuce-windows-${project.version}.jar")
        println("\nTo run on Windows:")
        println("1. Install Java 17 or higher")
        println("2. Run: java -jar dongadeuce-windows-${project.version}.jar")
    }
}

// Task to download Launch4j if not present
tasks.register("downloadLaunch4j") {
    group = "distribution"
    description = "Downloads Launch4j binaries"

    val launch4jDir = file("build/launch4j-bin")
    val launch4jVersion = "3.50"
    val launch4jZip = file("build/launch4j.zip")
    val launch4jExecutable = file("build/launch4j-bin/launch4j/launch4jc")

    outputs.dir(launch4jDir)

    doLast {
        if (!launch4jExecutable.exists()) {
            println("Downloading Launch4j $launch4jVersion...")

            val url = "https://sourceforge.net/projects/launch4j/files/launch4j-3/$launch4jVersion/launch4j-$launch4jVersion-linux-x64.tgz/download"

            ant.invokeMethod("get", mapOf(
                "src" to url,
                "dest" to launch4jZip,
                "verbose" to true
            ))

            println("Extracting Launch4j...")
            launch4jDir.mkdirs()

            ant.invokeMethod("untar", mapOf(
                "src" to launch4jZip,
                "dest" to launch4jDir,
                "compression" to "gzip"
            ))

            // Make executables
            launch4jExecutable.setExecutable(true)
            file("build/launch4j-bin/launch4j/bin/windres").setExecutable(true)
            file("build/launch4j-bin/launch4j/bin/ld").setExecutable(true)

            // Clean up
            launch4jZip.delete()

            println("Launch4j installed at: ${launch4jDir.absolutePath}")
        } else {
            println("Launch4j already downloaded")
        }
    }
}

// Task to create Windows EXE using Launch4j directly
tasks.register<Exec>("createWindowsExe") {
    group = "distribution"
    description = "Creates a Windows EXE from the JAR using Launch4j (no Docker required)"

    dependsOn("packageWindowsJar", "downloadLaunch4j")

    workingDir(projectDir)

    val jarFile = file("build/windows/dongadeuce-windows-${project.version}.jar")
    val exeFile = file("build/windows/dongadeuce.exe")
    val configFile = file("build/launch4j-config/config.xml")

    doFirst {
        // Create the Launch4j configuration file
        val configDir = file("build/launch4j-config")
        configDir.mkdirs()

        configFile.writeText("""
<?xml version="1.0" encoding="UTF-8"?>
<launch4jConfig>
    <dontWrapJar>false</dontWrapJar>
    <headerType>gui</headerType>
    <jar>${jarFile.absolutePath}</jar>
    <outfile>${exeFile.absolutePath}</outfile>
    <errTitle>dongadeuce</errTitle>
    <cmdLine></cmdLine>
    <chdir>.</chdir>
    <priority>normal</priority>
    <downloadUrl>https://adoptium.net/</downloadUrl>
    <supportUrl></supportUrl>
    <stayAlive>false</stayAlive>
    <restartOnCrash>false</restartOnCrash>
    <manifest></manifest>
    <icon>${projectDir.absolutePath}/../resources/dongadeuce_icon.ico</icon>
    <classPath>
        <mainClass>com.dustinmcafee.dongadeuce.MainKt</mainClass>
    </classPath>
    <jre>
        <path></path>
        <bundledJre64Bit>true</bundledJre64Bit>
        <bundledJreAsFallback>false</bundledJreAsFallback>
        <minVersion>17</minVersion>
        <maxVersion></maxVersion>
        <jdkPreference>preferJre</jdkPreference>
        <runtimeBits>64</runtimeBits>
        <initialHeapSize>512</initialHeapSize>
        <maxHeapSize>2048</maxHeapSize>
    </jre>
    <versionInfo>
        <fileVersion>${project.version}.0</fileVersion>
        <txtFileVersion>${project.version}</txtFileVersion>
        <fileDescription>Commander MTG Game</fileDescription>
        <copyright>Copyright 2024</copyright>
        <productVersion>${project.version}.0</productVersion>
        <txtProductVersion>${project.version}</txtProductVersion>
        <productName>dongadeuce</productName>
        <companyName>Dustin McAfee</companyName>
        <internalName>dongadeuce</internalName>
        <originalFilename>dongadeuce.exe</originalFilename>
    </versionInfo>
</launch4jConfig>
        """.trimIndent())

        println("Launch4j configuration created")
        println("Input JAR: ${jarFile.absolutePath}")
        println("Output EXE: ${exeFile.absolutePath}")
    }

    // Call launch4j.jar directly with java instead of using the shell script
    val launch4jJar = file("build/launch4j-bin/launch4j/launch4j.jar")

    commandLine(
        "java",
        "-Djava.awt.headless=true",
        "-jar",
        launch4jJar.absolutePath,
        configFile.absolutePath
    )

    doLast {
        if (exeFile.exists()) {
            println("\n✓ Windows EXE created successfully!")
            println("  Location: ${exeFile.absolutePath}")
            println("  Size: ${exeFile.length() / 1024 / 1024} MB")
            println("\nThe EXE requires Java 17+ to be installed on the target Windows machine.")
        } else {
            println("\n✗ Failed to create Windows EXE")
        }
    }
}
