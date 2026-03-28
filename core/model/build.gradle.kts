plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Pure JVM module — use coroutines-core (not the Android artifact)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
