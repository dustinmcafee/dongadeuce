plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
}

allprojects {
    group = "com.commandermtg"
    version = "2.10.3"
}
