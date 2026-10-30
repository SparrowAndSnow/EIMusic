package com.eimsound.eimusic.server.db

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object ServerDatabase {
    private val logger = LoggerFactory.getLogger("ServerDatabase")

    fun connect(path: Path = configuredPath()): Database {
        path.toAbsolutePath().parent?.let { Files.createDirectories(it) }

        val database = Database.connect(
            url = "jdbc:sqlite:${path.toAbsolutePath()}",
            driver = "org.sqlite.JDBC",
        )
        transaction(database) {
            SchemaUtils.create(Users, SourceUrls, AdminAudit)
            SchemaUtils.createMissingTablesAndColumns(Users, SourceUrls, AdminAudit)
        }
        logger.info("SQLite database ready: {}", path.toAbsolutePath())
        return database
    }

    /** 可通过 EIMUSIC_DB 覆盖；默认放在平台配置目录，避免写进仓库。 */
    fun configuredPath(): Path {
        System.getenv("EIMUSIC_DB")?.takeIf { it.isNotBlank() }?.let {
            return Path.of(it)
        }

        val os = System.getProperty("os.name").lowercase()
        val home = Path.of(System.getProperty("user.home"))
        val dir = when {
            os.contains("win") -> Path.of(System.getenv("APPDATA") ?: home.toString(), "eimusic")
            os.contains("mac") -> home.resolve("Library/Application Support/eimusic")
            else -> home.resolve(".config/eimusic")
        }
        return dir.resolve("server.db")
    }
}
