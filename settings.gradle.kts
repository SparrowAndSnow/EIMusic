rootProject.name = "EIMusic"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {

        maven { url = uri("https://www.jitpack.io") }
//        maven { url = uri("https://maven.aliyun.com/repository/releases") }
//        maven { url = uri("https://maven.aliyun.com/repository/google") }
//        maven { url = uri("https://maven.aliyun.com/repository/central") }
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://www.jitpack.io") }
//        maven { url = uri("https://maven.aliyun.com/repository/releases") }
//        maven { url = uri("https://maven.aliyun.com/repository/google") }
//        maven { url = uri("https://maven.aliyun.com/repository/central") }
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
        google()
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
val includeClient = System.getProperty("includeClient") ?: "true"
if (includeClient.toBoolean()) {
    include(":composeApp")
}
include(":shared")
include("server")
include("terminalApp")
includeBuild("./mosaic")