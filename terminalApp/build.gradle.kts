import java.io.FileInputStream
import java.net.URI
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import java.nio.channels.FileChannel
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

abstract class FetchNativeSource : DefaultTask() {
    @get:Input @get:Optional abstract val archiveUrl: org.gradle.api.provider.Property<String>
    @get:Input @get:Optional abstract val archiveSha256: org.gradle.api.provider.Property<String>
    @get:Input @get:Optional abstract val archiveName: org.gradle.api.provider.Property<String>
    @get:Input @get:Optional abstract val filesInArchive: org.gradle.api.provider.ListProperty<String>
    @get:Input abstract val outputSha256: org.gradle.api.provider.MapProperty<String, String>
    @get:Input abstract val directFiles: org.gradle.api.provider.MapProperty<String, String>
    @get:OutputDirectory abstract val destination: DirectoryProperty
    @get:Input abstract val offline: org.gradle.api.provider.Property<Boolean>

    @TaskAction
    fun fetch() {
        val dest = destination.get().asFile.toPath()
        val parent = dest.parent
        Files.createDirectories(parent)
        val lockPath = parent.resolve(".${dest.fileName}.lock")
        FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use {
                if (hasValidCache(dest)) return
                check(!offline.get()) {
                    "Pinned native source cache is missing or invalid for ${dest.fileName}. " +
                        "Run ${path} once online, then retry with --offline."
                }
                val stage = Files.createTempDirectory(parent, ".${dest.fileName}.stage-")
                try {
                    val expected = filesInArchive.get().toSet()
                    if (directFiles.get().isNotEmpty()) {
                        directFiles.get().forEach { (name, url) ->
                            download(url, stage.resolve(name))
                        }
                    } else {
                        val archive = stage.resolve(archiveName.get())
                        download(archiveUrl.get(), archive)
                        val actual = sha256(archive)
                        check(actual.equals(archiveSha256.get(), ignoreCase = true)) {
                            "SHA-256 mismatch for ${archiveUrl.get()}: expected ${archiveSha256.get()}, got $actual"
                        }
                        ZipInputStream(Files.newInputStream(archive)).use { zip ->
                            while (true) {
                                val entry = zip.nextEntry ?: break
                                val name = entry.name.substringAfterLast('/')
                                if (!entry.isDirectory && name in expected) {
                                    Files.copy(zip, stage.resolve(name))
                                }
                                zip.closeEntry()
                            }
                        }
                    }
                    check(expectedOutputsValid(stage)) {
                        "Downloaded files failed SHA-256 validation for $path"
                    }
                    archiveName.orNull?.let { Files.deleteIfExists(stage.resolve(it)) }
                    if (Files.exists(dest)) dest.toFile().deleteRecursively()
                    try {
                        Files.move(stage, dest, StandardCopyOption.ATOMIC_MOVE)
                    } catch (_: Exception) {
                        Files.move(stage, dest)
                    }
                } finally {
                    if (Files.exists(stage)) stage.toFile().deleteRecursively()
                }
            }
        }
    }

    private fun download(url: String, destination: java.nio.file.Path) {
        val connection = URI(url).toURL().openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        if (connection is HttpURLConnection) {
            connection.instanceFollowRedirects = true
            connection.connect()
        }
        connection.getInputStream().use { input -> Files.copy(input, destination) }
    }

    private fun sha256(path: java.nio.file.Path): String = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path)).joinToString("") { "%02x".format(it) }

    private fun expectedOutputsValid(directory: java.nio.file.Path): Boolean = outputSha256.get().all { (name, hash) ->
        val file = directory.resolve(name)
        Files.isRegularFile(file) && sha256(file).equals(hash, ignoreCase = true)
    }

    private fun hasValidCache(directory: java.nio.file.Path): Boolean = expectedOutputsValid(directory)
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

val cinteropDir = project.file("src/nativeInterop/cinterop")
val nativeCacheRoot = gradle.gradleUserHomeDir.resolve("caches/eimusic-native")
val miniaudioCacheDir = nativeCacheRoot.resolve("miniaudio/0.11.25")
val sqliteCacheDir = nativeCacheRoot.resolve("sqlite/3.54.1")
val anyasciiCacheDir = nativeCacheRoot.resolve("anyascii/389992a997665f77aced7e09839c9d1de0296d4f")

/** Fetches a pinned source artifact, verifies it, and atomically publishes selected files to the Gradle cache. */

val prepareMiniaudioSources = tasks.register("prepareMiniaudioSources", FetchNativeSource::class) {
    directFiles.set(mapOf("miniaudio.h" to "https://raw.githubusercontent.com/mackron/miniaudio/0.11.25/miniaudio.h"))
    filesInArchive.set(emptyList())
    outputSha256.set(mapOf("miniaudio.h" to "ac7af4de748b7e26b777f37e01cee313a308a7296a3eb080e2906b320cc55c89"))
    destination.set(miniaudioCacheDir)
    offline.set(gradle.startParameter.isOffline)
}
val prepareSqliteSources = tasks.register("prepareSqliteSources", FetchNativeSource::class) {
    archiveUrl.set("https://www.sqlite.org/2026/sqlite-amalgamation-3540100.zip")
    archiveSha256.set("0a9c8e34352dad46006aa82836d5383bff9dc6bc9ce5a60553c05aa30e6ad1f9")
    archiveName.set("sqlite-amalgamation-3540100.zip")
    filesInArchive.set(listOf("sqlite3.c", "sqlite3.h"))
    directFiles.set(emptyMap())
    outputSha256.set(mapOf(
        "sqlite3.c" to "0c657666d819f261cc4007526ad57e1669de9949df7a8ea847aa284a2bf0ee52",
        "sqlite3.h" to "d6c84557261893439b341d2e6c35187ae3ea24fbfd843ad94616f72d89242b8c"
    ))
    destination.set(sqliteCacheDir)
    offline.set(gradle.startParameter.isOffline)
}
val prepareAnyasciiSources = tasks.register("prepareAnyasciiSources", FetchNativeSource::class) {
    directFiles.set(mapOf(
        "anyascii.c" to "https://raw.githubusercontent.com/anyascii/anyascii/389992a997665f77aced7e09839c9d1de0296d4f/impl/c/anyascii.c",
        "anyascii.h" to "https://raw.githubusercontent.com/anyascii/anyascii/389992a997665f77aced7e09839c9d1de0296d4f/impl/c/anyascii.h"
    ))
    filesInArchive.set(emptyList())
    outputSha256.set(mapOf(
        "anyascii.c" to "c6ff0752d807ad9d7d0b3f34a6ff869fee37da6ef7bc5ab2a44de529c8e35dc7",
        "anyascii.h" to "f9aab097bc0fd13d9bba6f1754e1b7ede1b41a683f68de038a75a68814716ef1"
    ))
    destination.set(anyasciiCacheDir)
    offline.set(gradle.startParameter.isOffline)
}

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
            implementation("com.github.SparrowAndSnow.mosaic:mosaic-runtime:0.19.1")
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
                linkerOpts.add("-L/usr/lib")
                linkerOpts.add("-L/usr/lib/x86_64-linux-gnu")
                linkerOpts.add("-lsqlite3")
                linkerOpts.add("-lsystemd")
                linkerOpts.add("--allow-shlib-undefined")
            }
        }
        compilations["main"].cinterops {
            val miniaudio by creating {
                definitionFile.set(cinteropDir.resolve("miniaudio.def"))
                compilerOpts.add("-I${miniaudioCacheDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.addAll(listOf("-I${anyasciiCacheDir.absolutePath}", "-I${cinteropDir.absolutePath}"))
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
                compilerOpts.add("-I${miniaudioCacheDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.addAll(listOf("-I${anyasciiCacheDir.absolutePath}", "-I${cinteropDir.absolutePath}"))
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
                compilerOpts.add("-I${miniaudioCacheDir.absolutePath}")
            }
            val anyascii by creating {
                definitionFile.set(cinteropDir.resolve("anyascii.def"))
                compilerOpts.addAll(listOf("-I${anyasciiCacheDir.absolutePath}", "-I${cinteropDir.absolutePath}"))
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
                "$toolchain -I\"${anyasciiCacheDir.absolutePath}\" -c -O2 $cflags \"${anyasciiCacheDir.resolve("anyascii.c").absolutePath}\" -o anyascii.o && " +
                "$toolchain -I\"${anyasciiCacheDir.absolutePath}\" -c -O2 $cflags anyascii_helper.c -o anyascii_helper.o && " +
                "ar rcs libanyascii.a anyascii.o anyascii_helper.o"
            )
        } else {
            commandLine("sh", "-c",
                "$toolchain -I\"${anyasciiCacheDir.absolutePath}\" -c -O2 $cflags \"${anyasciiCacheDir.resolve("anyascii.c").absolutePath}\" -o anyascii.o && " +
                "$toolchain -I\"${anyasciiCacheDir.absolutePath}\" -c -O2 $cflags anyascii_helper.c -o anyascii_helper.o && " +
                "ar rcs libanyascii.a anyascii.o anyascii_helper.o"
            )
        }
        inputs.files(anyasciiCacheDir.resolve("anyascii.c"), anyasciiCacheDir.resolve("anyascii.h"),
            dir.resolve("anyascii_helper.c"), dir.resolve("anyascii_helper.h"))
        dependsOn(prepareAnyasciiSources)
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
                "$toolchain -I\"${miniaudioCacheDir.absolutePath}\" -c -O2 $cflags miniaudio.c -o miniaudio.o && " +
                "ar rcs libminiaudio.a miniaudio.o"
            )
        } else {
            commandLine("sh", "-c",
                "$toolchain -I\"${miniaudioCacheDir.absolutePath}\" -c -O2 $cflags miniaudio.c -o miniaudio.o && " +
                "ar rcs libminiaudio.a miniaudio.o"
            )
        }
        inputs.file(dir.resolve("miniaudio.c"))
        inputs.file(miniaudioCacheDir.resolve("miniaudio.h"))
        outputs.file(dir.resolve("libminiaudio.a"))
    }

val compileMiniaudioLinux = compileMiniaudioTask("compileMiniaudioLinux", "Linux", "gcc", "-fPIC")
val compileMiniaudioMacosArm64 = compileMiniaudioTask("compileMiniaudioMacosArm64", "macOS", "clang", "")
val compileMiniaudioMingwX64 = compileMiniaudioTask("compileMiniaudioMingwX64", "Windows", "x86_64-w64-mingw32-gcc", "")

/** Compile the downloaded SQLite amalgamation into a static library for Windows. */
val compileSqliteMingwX64 = tasks.register("compileSqliteMingwX64", Exec::class) {
    val dir = project.file("src/nativeInterop/cinterop")
    workingDir = dir
    description = "Compiles sqlite3.c into libsqlite3.a (Windows)"
    commandLine(
        "cmd", "/c",
        "x86_64-w64-mingw32-gcc -c -O2 -DSQLITE_THREADSAFE=1 -DSQLITE_ENABLE_COLUMN_METADATA " +
            "\"${sqliteCacheDir.resolve("sqlite3.c").absolutePath}\" -o sqlite3.o && " +
            "ar rcs \"${dir.resolve("libsqlite3.a").absolutePath}\" sqlite3.o"
    )
    inputs.files(sqliteCacheDir.resolve("sqlite3.c"), sqliteCacheDir.resolve("sqlite3.h"))
    outputs.files(dir.resolve("libsqlite3.a"), dir.resolve("sqlite3.o"))
    dependsOn(prepareSqliteSources)
}

tasks.matching { it.name.startsWith("cinteropMiniaudio") }.configureEach {
    dependsOn(prepareMiniaudioSources)
    inputs.file(miniaudioCacheDir.resolve("miniaudio.h"))
}

tasks.matching { it.name.startsWith("cinteropAnyascii") }.configureEach {
    dependsOn(prepareAnyasciiSources)
    inputs.files(anyasciiCacheDir.resolve("anyascii.h"), cinteropDir.resolve("anyascii_helper.h"))
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

tasks.configureEach {
    if (name.startsWith("compileMiniaudio")) dependsOn(prepareMiniaudioSources)
}
