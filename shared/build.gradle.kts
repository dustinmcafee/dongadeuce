plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.library")
}

kotlin {
    jvm()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val ktorVersion = "2.3.7"
        val coroutinesVersion = "1.7.3"
        val serializationVersion = "1.6.2"

        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-websockets:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            implementation("io.ktor:ktor-server-core:$ktorVersion")
            implementation("io.ktor:ktor-server-websockets:$ktorVersion")
            implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:$ktorVersion")
            implementation("io.ktor:ktor-server-netty:$ktorVersion")
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("io.ktor:ktor-server-cio:$ktorVersion")
            implementation("androidx.datastore:datastore-preferences:1.0.0")
        }
    }
}

android {
    namespace = "com.dustinmcafee.dongadeuce.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
