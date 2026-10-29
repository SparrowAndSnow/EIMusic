import java.io.FileInputStream
import org.tomlj.Toml
import org.tomlj.TomlParseResult

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

group = "com.eimsound.eimusic"

fun loadVersionToml(): TomlParseResult {
    val file = file("../version.toml")
    file.exists() || throw Error("version.toml is not found")
    val toml = Toml.parse(FileInputStream(file)) ?: throw Error("version.toml is not valid")
    return toml
}

val versionToml = loadVersionToml()
val appVersionName =
    versionToml.getString("version.name") ?: throw Error("version.name is not defined in version.toml")
val appVersionCode = versionToml.getLong("version.code")?.toInt()
    ?: throw Error("version.code is not defined in version.toml")

version = appVersionName

buildConfig {
    useKotlinOutput()

    packageName("com.eimsound.eimusic")
    buildConfigField("VERSION_NAME", appVersionName)
    buildConfigField("VERSION_CODE", appVersionCode)
    buildConfigField("APP_NAME", rootProject.name)
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/firework/dev")
    google()
    mavenCentral()
    gradlePluginPortal()
}

val cinteropDir = project.file("src/nativeInterop/cinterop")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    linuxX64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation("com.jakewharton.mosaic:mosaic-runtime:0.19.0-SNAPSHOT")
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io)
        }

        nativeMain.dependencies {
            implementation(libs.atomicfu)
            implementation(libs.ktor.client.curl)
            implementation(libs.sqldelight.native.driver)
        }

        // Platform-specific MPRIS only on Linux
        linuxX64Main.dependencies {
            implementation("com.monkopedia:sdbus-kotlin:1.0.1")
        }
    }

    // ── Linux target ──
    linuxX64().apply {
        binaries {
            executable {
                baseName = "eimusic"
                entryPoint = "com.eimsound.eimusic.main"
                linkerOpts.add("-L${cinteropDir.absolutePath}")
                linkerOpts.add("-lminiaudio")
                linkerOpts.add("-lanyascii")
                linkerOpts.add("-lm")
                linkerOpts.add("-ldl")
                linkerOpts.add("-lpthread")
                linkerOpts.add("-lsqlite3")
                linkerOpts.add("-L/usr/lib")
                linkerOpts.add("-lsystemd")
                linkerOpts.add("--allow-shlib-undefined")
            }
        }
        compilations["main"].cinterops {
            val miniaudio by creating {
                definitionFile.set(cinteropDir.resolve("miniaudio.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
        }
    }

    // ── macOS target ──
    macosArm64().apply {
        binaries {
            executable {
                baseName = "eimusic"
                entryPoint = "com.eimsound.eimusic.main"
                linkerOpts.add("-L${cinteropDir.absolutePath}")
                linkerOpts.add("-lminiaudio")
                linkerOpts.add("-lanyascii")
                linkerOpts.add("-lm")
                linkerOpts.add("-lpthread")
                linkerOpts.add("-lsqlite3")
            }
        }
        compilations["main"].cinterops {
            val miniaudio by creating {
                definitionFile.set(cinteropDir.resolve("miniaudio.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
        }
    }

    // ── Windows target ──
    mingwX64().apply {
        binaries {
            executable {
                baseName = "eimusic"
                entryPoint = "com.eimsound.eimusic.main"
                linkerOpts.add("-L${cinteropDir.absolutePath}")
                linkerOpts.add("-lminiaudio")
                linkerOpts.add("-lanyascii")
                // Windows 没有系统 libsqlite3，使用下方 compileSqliteMingwX64 构建的静态库
                linkerOpts.add("-lsqlite3")
            }
        }
        compilations["main"].cinterops {
            val miniaudio by creating {
                definitionFile.set(cinteropDir.resolve("miniaudio.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.add("-I${cinteropDir.absolutePath}")
            }
        }
    }
}

/** Compile anyascii C sources into a static library for each target platform. */
val anyasciiDir = project.file("src/nativeInterop/cinterop")
fun compileAnyasciiTask(name: String, desc: String, toolchain: String, cflags: String) =
    tasks.register(name, Exec::class) {
        val dir = anyasciiDir
        description = "Compiles anyascii C sources into libanyascii.a ($desc)"
        workingDir = dir
        // Use cmd /c for Windows, sh -c for Unix-like systems
        if (name.contains("Mingw")) {
            commandLine("cmd", "/c",
                "$toolchain -c -O2 $cflags anyascii.c -o anyascii.o && " +
                "$toolchain -c -O2 $cflags anyascii_helper.c -o anyascii_helper.o && " +
                "ar rcs libanyascii.a anyascii.o anyascii_helper.o"
            )
        } else {
            commandLine("sh", "-c",
                "$toolchain -c -O2 $cflags anyascii.c -o anyascii.o && " +
                "$toolchain -c -O2 $cflags anyascii_helper.c -o anyascii_helper.o && " +
                "ar rcs libanyascii.a anyascii.o anyascii_helper.o"
            )
        }
        inputs.files(dir.resolve("anyascii.c"), dir.resolve("anyascii_helper.c"))
        outputs.file(dir.resolve("libanyascii.a"))
    }

val compileAnyasciiLinux = compileAnyasciiTask("compileAnyasciiLinux", "Linux", "gcc", "-fPIC")
val compileAnyasciiMacosArm64 = compileAnyasciiTask("compileAnyasciiMacosArm64", "macOS", "clang", "")
val compileAnyasciiMingwX64 = compileAnyasciiTask("compileAnyasciiMingwX64", "Windows", "x86_64-w64-mingw32-gcc", "")

tasks.matching { it.name.startsWith("link") && it.name.contains("LinuxX64") }.configureEach {
    dependsOn(compileAnyasciiLinux)
}
tasks.matching { it.name.startsWith("link") && it.name.contains("MacosArm64") }.configureEach {
    dependsOn(compileAnyasciiMacosArm64)
}
tasks.matching { it.name.startsWith("link") && it.name.contains("MingwX64") }.configureEach {
    dependsOn(compileAnyasciiMingwX64)
}

/** Compile miniaudio C source into a static library for each target platform. */
fun compileMiniaudioTask(name: String, platform: String, toolchain: String, cflags: String) =
    tasks.register(name, Exec::class) {
        val dir = project.file("src/nativeInterop/cinterop")
        description = "Compiles miniaudio.c into libminiaudio.a ($platform)"
        workingDir = dir
        // Use cmd /c for Windows, sh -c for Unix-like systems
        if (name.contains("Mingw")) {
            commandLine("cmd", "/c",
                "$toolchain -c -O2 $cflags miniaudio.c -o miniaudio.o && " +
                "ar rcs libminiaudio.a miniaudio.o"
            )
        } else {
            commandLine("sh", "-c",
                "$toolchain -c -O2 $cflags miniaudio.c -o miniaudio.o && " +
                "ar rcs libminiaudio.a miniaudio.o"
            )
        }
        inputs.files(dir.resolve("miniaudio.c"), dir.resolve("miniaudio.h"))
        outputs.file(dir.resolve("libminiaudio.a"))
    }

val compileMiniaudioLinux = compileMiniaudioTask("compileMiniaudioLinux", "Linux", "gcc", "-fPIC")
val compileMiniaudioMacosArm64 = compileMiniaudioTask("compileMiniaudioMacosArm64", "macOS", "clang", "")
val compileMiniaudioMingwX64 = compileMiniaudioTask("compileMiniaudioMingwX64", "Windows", "x86_64-w64-mingw32-gcc", "")

/** Compile the vendored SQLite amalgamation into a static library for Windows (no system libsqlite3 there). */
val compileSqliteMingwX64 = tasks.register("compileSqliteMingwX64", Exec::class) {
    val dir = project.file("src/nativeInterop/cinterop")
    description = "Compiles sqlite3.c into libsqlite3.a (Windows)"
    workingDir = dir
    commandLine(
        "cmd", "/c",
        "x86_64-w64-mingw32-gcc -c -O2 -DSQLITE_THREADSAFE=1 -DSQLITE_ENABLE_COLUMN_METADATA " +
            "sqlite3.c -o sqlite3.o && ar rcs libsqlite3.a sqlite3.o"
    )
    inputs.files(dir.resolve("sqlite3.c"), dir.resolve("sqlite3.h"))
    outputs.file(dir.resolve("libsqlite3.a"))
}

tasks.matching { it.name.startsWith("link") && it.name.contains("LinuxX64") }.configureEach {
    dependsOn(compileMiniaudioLinux)
}
tasks.matching { it.name.startsWith("link") && it.name.contains("MacosArm64") }.configureEach {
    dependsOn(compileMiniaudioMacosArm64)
}
tasks.matching { it.name.startsWith("link") && it.name.contains("MingwX64") }.configureEach {
    dependsOn(compileMiniaudioMingwX64)
    dependsOn(compileSqliteMingwX64)
}
