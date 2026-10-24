import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import java.io.FileInputStream
import org.tomlj.Toml
import org.tomlj.TomlParseResult

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
//    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.osdetector") version "1.7.3"

    // 添加 KMP BuildConfig 插件支持
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    google()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    android {
        namespace = "com.eimsound.eimusic.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(), iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.bundles.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.filekit)
//            implementation(compose.runtime)
//            implementation(compose.foundation)
//            implementation(compose.material3)
//            implementation(compose.ui)
//            implementation(compose.components.resources)
//            implementation(compose.components.uiToolingPreview)
//            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)

            implementation(libs.coil.compose.image.loader)
            implementation(libs.coil.compose.image.loader.network.ktor)
            implementation(libs.bundles.koin)
            implementation(libs.atomicfu)
            implementation(libs.bundles.navigation)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.common)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.navigation.event)
            implementation(libs.androidx.media3.session)
            implementation(libs.koin.android)
            implementation(libs.androidx.lifecycle.process)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

//            val javafxPlatform = when (osdetector.classifier) {
//                "linux-x86_64" -> "linux"
//                "linux-aarch_64" -> "linux-aarch64"
//                "windows-x86_64" -> "win"
//                "osx-x86_64" -> "mac"
//                "osx-aarch_64" -> "mac-aarch64"
//                else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
//            }
//            val javaFxVersion = "25"
//            implementation("org.openjfx:javafx-media:$javaFxVersion:${javafxPlatform}")
//            implementation("org.openjfx:javafx-graphics:$javaFxVersion:${javafxPlatform}")

//            implementation("org.openjfx:javafx-base:$javaFxVersion:${javafxPlatform}")
//            implementation("org.openjfx:javafx-controls:$javaFxVersion:${javafxPlatform}")
//            implementation("org.openjfx:javafx-swing:$javaFxVersion:${javafxPlatform}")
            implementation(libs.jna.platform)
            implementation(libs.jna)
            implementation(files("./libs/java-stream-player-10.0.0.jar"))
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
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

version = appVersionName
val appVersionCode = versionToml.getLong("version.code")?.toInt()
    ?: throw Error("version.code is not defined in version.properties")

buildConfig {
    useKotlinOutput()

    packageName("com.eimsound.eimusic")
    buildConfigField("VERSION_NAME", appVersionName)
    buildConfigField("VERSION_CODE", appVersionCode)
    buildConfigField("APP_NAME", rootProject.name)
    buildConfigField("OS", osdetector.classifier)
}


compose.desktop {
    application {
        mainClass = "com.eimsound.eimusic.MainKt"
        jvmArgs(
            "--enable-native-access=ALL-UNNAMED"
        )
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = rootProject.name
            packageVersion = appVersionName
//            modules("javafx.graphics", "javafx.media")
            linux {
                packageName = rootProject.name
            }
            windows {
                menuGroup = rootProject.name
                shortcut = true
                console = false // 设置为 false 避免运行时出现黑框
            }
            macOS {
                bundleID = "com.eimsound.eimusic"
                packageName = rootProject.name
                signing {
                    sign.set(true)
                    identity.set("Developer ID Application: <>")
                }
                notarization {
                    appleID.set(System.getenv("APPLE_ID") ?: "")
                    password.set(System.getenv("APP_PASSWORD") ?: "")
                    teamID.set("H53Q22J4T3")
                }
            }
        }
        buildTypes.release {
            proguard {
                version.set("7.8.2")
                configurationFiles.from("proguard-config.pro")
                configurationFiles.from("proguard-rules.desktop.pro")
                configurationFiles.from("proguard-rules-javastream.desktop.pro")
            }
        }
    }
    var createReleaseDistributableWithStartupShell = tasks.register("createReleaseDistributableWithStartupShell") {
        group = "compose desktop"
        description = "打包并压缩 Release 分发添加启动 shell 文件"
        // 确保在打包前已经生成了分发文件
        dependsOn("createReleaseDistributable")
        doLast {
            val buildDirectory = layout.buildDirectory.get().asFile

            val shellName = "startup.sh"
            val binDir = File(buildDirectory, "compose/binaries/main-release/app/${rootProject.name}/bin")
            val shell = File(binDir, shellName)

            val wrapperScript = rootProject.file("scripts/${shellName}")
            wrapperScript.copyTo(shell, overwrite = true)
            println("脚本已成功包装到 bin 目录中！")
        }
    }
    // 定义一个压缩任务
    val createReleaseDistributableZip = tasks.register<Zip>("createReleaseDistributableZip") {
        group = "compose desktop"
        description = "打包并压缩 Release 分发包为 ZIP 文件"

        // 设置压缩包的文件名
        archiveFileName.set("${rootProject.name}-${appVersionName}-${osdetector.classifier}.zip")

        // 设置输出目录
        destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main-release/app"))

        // 设置源码：即 createReleaseDistributable 生成的内容目录
        // 路径通常为 build/compose/binaries/main-release/app
        from(layout.buildDirectory.dir("compose/binaries/main-release/app/${rootProject.name}"))

        // 确保在打包前已经生成了分发文件
        dependsOn("createReleaseDistributable")
    }
}
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}