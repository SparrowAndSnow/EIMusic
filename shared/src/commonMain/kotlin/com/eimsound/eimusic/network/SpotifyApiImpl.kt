package com.eimsound.eimusic.network

import com.eimsound.eimusic.SPOTIFY_TOKEN
import com.eimsound.eimusic.network.models.featuredplaylist.*
import com.eimsound.eimusic.network.models.newreleases.*
import com.eimsound.eimusic.network.models.topfiftycharts.*
import com.eimsound.eimusic.sampledata.newReleases
import com.eimsound.eimusic.sampledata.topFiftyChartsResponse
import com.eimsound.eimusic.sampledata.featurePlaylistResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json



class SpotifyApiImpl : SpotifyApi {
    override suspend fun getTopFiftyChart(): TopFiftyCharts {
        if (SPOTIFY_TOKEN.isEmpty()) {
            return Json.decodeFromString<TopFiftyCharts>(topFiftyChartsResponse)
        }
        return client.get {
            headers {
                spotifyEndPoint("v1/playlists/37i9dQZEVXbMDoHDwVN2tF")
            }
        }.body()
    }

    override suspend fun getNewReleases(): NewReleasedAlbums {
        if (SPOTIFY_TOKEN.isEmpty()) {
            return Json.decodeFromString<NewReleasedAlbums>(newReleases)
        }
        return client.get {
            headers {
                spotifyEndPoint("v1/browse/new-releases")
            }
        }.body()
    }

    override suspend fun getFeaturedPlaylist(): FeaturedPlayList {
        if (SPOTIFY_TOKEN.isEmpty()) {
            return Json.decodeFromString<FeaturedPlayList>(featurePlaylistResponse)
        }
        return client.get {
            headers {
                spotifyEndPoint("v1/browse/featured-playlists")
            }
        }.body()
    }

    override suspend fun getPlayList(playlistId: String): TopFiftyCharts {
        if (SPOTIFY_TOKEN.isEmpty()) {
            return Json.decodeFromString<TopFiftyCharts>(topFiftyChartsResponse)
        }
        return client.get {
            headers {
                spotifyEndPoint("v1/playlists/$playlistId")
            }
        }.body()
    }

    private val client = HttpClient {
        expectSuccess = true
        install(HttpTimeout) {
            val timeout = 30000L
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
    }

    private fun HttpRequestBuilder.spotifyEndPoint(path: String) {
        url {
            takeFrom("https://api.spotify.com/v1/")
            encodedPath = path
            headers {
                append(
                    HttpHeaders.Authorization, SPOTIFY_TOKEN
                )
            }
        }
    }
}

