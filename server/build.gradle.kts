import java.io.FileInputStream
import org.tomlj.Toml
import org.tomlj.TomlParseResult

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
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
    versionToml.getString("version.name") ?: throw Error("version.name is not defined in version.toml")

group = "com.eimsound"
version = appVersionName

application {
    mainClass = "com.eimsound.eimusic.server.ApplicationKt"
}

kotlin {
    jvmToolchain(25)
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
