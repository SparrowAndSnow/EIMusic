import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
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
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.navigation)
            implementation(libs.androidx.navigation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(projects.shared)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(libs.coil.compose.image.loader)
            implementation(libs.coil.compose.image.loader.network.ktor)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "com.eimsound.eimusic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eimsound.eimusic"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.eimsound.eimusic.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.eimsound.eimusic"
            packageVersion = "1.0.0"
        }
        buildTypes.release {
            proguard {
                configurationFiles.from("proguard-config.pro")
            }
        }
    }
}
composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}
