package com.eimsound.eimusic.server.db

import com.eimsound.eimusic.music.TrackSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SourceRepository(private val database: Database) {
    fun ensureDefaults() {
        transaction(database) {
            if (SourceUrls.selectAll().limit(1).toList().isEmpty()) {
                insertSources(DEFAULT_SOURCES)
            }
        }
    }

    fun load(): Map<TrackSource, List<String>> = transaction(database) {
        SourceUrls.selectAll()
            .map {
                SourceRow(
                    source = it[SourceUrls.sourceName],
                    url = it[SourceUrls.url],
                    position = it[SourceUrls.position],
                )
            }
            .mapNotNull { row ->
                TrackSource.entries.firstOrNull { it.name == row.source }?.let { it to row }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, rows) -> rows.sortedBy { it.position }.map { it.url } }
    }

    fun replace(sources: Map<TrackSource, List<String>>) {
        transaction(database) {
            SourceUrls.deleteAll()
            insertSources(sources)
        }
    }

    private fun insertSources(sources: Map<TrackSource, List<String>>) {
        sources.forEach { (source, urls) ->
            urls.forEachIndexed { index, url ->
                SourceUrls.insert {
                    it[SourceUrls.sourceName] = source.name
                    it[SourceUrls.url] = url
                    it[SourceUrls.position] = index
                }
            }
        }
    }

    private data class SourceRow(
        val source: String,
        val url: String,
        val position: Int,
    )

    companion object {
        val DEFAULT_SOURCES = mapOf(
            TrackSource.NETEASE to listOf("https://netease-source.com"),
        )
    }
}
