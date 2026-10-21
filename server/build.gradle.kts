import java.io.FileInputStream
import org.tomlj.Toml
import org.tomlj.TomlParseResult

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

repositories {
    maven { url = uri("https://www.jitpack.io") }
    maven { url = uri("https://maven.aliyun.com/repository/releases") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    google()
    mavenCentral()
    gradlePluginPortal()
}

fun loadVersionToml(): TomlParseResult {
    val file = file("../version.toml")
    file.exists() || throw Error("version.toml is not found")
    val toml = Toml.parse(FileInputStream(file)) ?: throw Error("version.toml is not valid")
    return toml
}

val versionToml = loadVersionToml()
val appVersionName =
    versionToml.getString("version.name") ?: throw Error("version.name is not defined in version.properties")
val appVersionCode = versionToml.getLong("version.code")?.toInt()
    ?: throw Error("version.code is not defined in version.properties")
val appVersionTitle =
    versionToml.getString("version.title") ?: throw Error("version.title is not defined in version.properties")
val appVersionDescription = versionToml.getString("version.description")
    ?: throw Error("version.description is not defined in version.properties")
val appVersionDownloadDesktopWindows = String.format(versionToml.getString("version.download.desktop.windows")
    ?: throw Error("version.download.desktop.windows is not defined in version.properties"), appVersionName, appVersionName)

val appVersionDownloadDesktopMac = String.format(versionToml.getString("version.download.desktop.mac")
    ?: throw Error("version.download.desktop.mac is not defined in version.properties"),appVersionName,appVersionName)

val appVersionDownloadDesktopLinux = String.format(versionToml.getString("version.download.desktop.linux")
    ?: throw Error("version.download.desktop.linux is not defined in version.properties"),appVersionName,appVersionName)
val appVersionDownloadAndroid = String.format(versionToml.getString("version.download.android")
    ?: throw Error("version.download.android is not defined in version.properties"),appVersionName,appVersionName)

val appVersionDownloadIOS = String.format(versionToml.getString("version.download.ios")
    ?: throw Error("version.download.ios is not defined in version.properties"),appVersionName,appVersionName)

val appVersionForce = versionToml.getBoolean("version.force")
    ?: throw Error("version.force is not defined in version.properties")

group = "com.eimsound"
version = appVersionName

application {
    mainClass = "com.eimsound.eimusic.server.ApplicationKt"
}

kotlin {
    jvmToolchain(25)
}

buildConfig {
    packageName("com.eimsound.eimusic.server")
    buildConfigField("String", "VERSION_NAME","\"$appVersionName\"")
    buildConfigField("Int", "VERSION_CODE", appVersionCode)
    buildConfigField("String", "VERSION_TITLE", "\"$appVersionTitle\"")
    buildConfigField("String", "VERSION_DESCRIPTION", "\"$appVersionDescription\"")
    buildConfigField("String", "VERSION_DOWNLOAD_DESKTOP_WINDOWS", "\"$appVersionDownloadDesktopWindows\"")
    buildConfigField("String", "VERSION_DOWNLOAD_DESKTOP_MAC", "\"$appVersionDownloadDesktopMac\"")
    buildConfigField("String", "VERSION_DOWNLOAD_DESKTOP_LINUX", "\"$appVersionDownloadDesktopLinux\"")
    buildConfigField("String", "VERSION_DOWNLOAD_ANDROID", "\"$appVersionDownloadAndroid\"")
    buildConfigField("String", "VERSION_DOWNLOAD_IOS", "\"$appVersionDownloadIOS\"")
    buildConfigField("Boolean", "VERSION_FORCE", appVersionForce)
}

dependencies {
    implementation(project(":shared"))
    implementation("io.insert-koin:koin-ktor:4.2.2")
    implementation("io.insert-koin:koin-logger-slf4j:4.2.2")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-serialization-kotlinx-protobuf")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("ch.qos.logback:logback-classic:1.6.5")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.4.20")

}
