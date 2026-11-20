rootProject.name = "EIMusic"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.SparrowAndSnow.mosaic")
            }
        }
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
include(":composeApp")
include(":androidApp")
include(":shared")
include("server")
include("terminalApp")
