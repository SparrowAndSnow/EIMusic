import java.io.FileInputStream
import java.util.Properties
import org.tomlj.Toml
import org.tomlj.TomlParseResult

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    google()
    mavenCentral()
}

fun loadVersionToml(): TomlParseResult {
    val file = file("../version.toml")
    file.exists() || throw Error("version.toml is not found")
    return Toml.parse(FileInputStream(file)) ?: throw Error("version.toml is not valid")
}

val versionToml = loadVersionToml()
val appVersionName =
    versionToml.getString("version.name") ?: throw Error("version.name is not defined in version.toml")
val appVersionCode = versionToml.getLong("version.code")?.toInt()
    ?: throw Error("version.code is not defined in version.toml")

fun localProperty(name: String): String? {
    val props = rootProject.file("local.properties")
    if (!props.exists()) return null
    return Properties().apply { props.inputStream().use { load(it) } }.getProperty(name)
}

version = appVersionName

android {
    namespace = "com.eimsound.eimusic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eimsound.eimusic"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    val keyStoreFile = localProperty("KEY_STORE_FILE")
    val keyStorePassword = localProperty("KEY_STORE_PASSWORD")
    val keyAlias = localProperty("KEY_ALIAS")
    val keyPassword = localProperty("KEY_PASSWORD")
    keyStoreFile?.let {
        signingConfigs {
            create("release") {
                storeFile = file(it)
                storePassword = keyStorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            keyStoreFile?.let {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("${rootProject.name}-${appVersionName}.apk")
        }
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.koin.android)
    implementation(libs.bundles.filekit)
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:${libs.versions.compose.multiplatform.get()}")
}
