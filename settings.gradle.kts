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
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
// 只构建服务端 / TUI 时可用 -DincludeClient=false 跳过客户端模块（server/Dockerfile 依赖这个开关）
val includeClient = System.getProperty("includeClient") ?: "true"
if (includeClient.toBoolean()) {
    include(":composeApp")
    include(":androidApp")
}
include(":shared")
include("server")
include("terminalApp")
// mosaic 复合构建：优先项目根目录，其次是相邻目录
val mosaicDir = listOf("mosaic", "../mosaic", "../../mosaic")
    .map { file(it) }
    .firstOrNull { it.isDirectory }
includeBuild(mosaicDir ?: file("mosaic"))