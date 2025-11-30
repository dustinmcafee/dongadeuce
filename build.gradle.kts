plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("multiplatform") version "1.9.21" apply false
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("org.jetbrains.compose") version "1.5.11" apply false
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
}

allprojects {
    group = "com.dustinmcafee.dongadeuce"
    version = "4.5.1"
}
