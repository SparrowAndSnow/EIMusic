package com.eimsound.eimusic.network

import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.network.models.netease.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.StateFlow

/**
 * 网易云音乐API实现类
 * 使用公开的网易云音乐API服务
 */
class NeteaseApiImpl(
    private val baseUrl: StateFlow<Url>,
    private val client: HttpClient
) : NeteaseApi {
    private suspend inline fun <reified T> handleHttpResponse(response: HttpResponse): NetworkResult<T> {
        return if (response.status.isSuccess()) {
            val body = response.body<NeteaseResult<T>>()
            if (body.success)
                NetworkResult.Success(body.data)
            else {
                NetworkResult.Error(response.status.value, body.message)
            }
        } else {
            NetworkResult.Error(response.status.value, "Server error: ${response.status.description}")
        }
    }

    override suspend fun search(
        keywords: String,
        limit: Int,
    ): NetworkResult<List<NeteaseSearch>> {
        return try {
            val response = client.post("${baseUrl.value}/Search") {
                setBody(
                    FormDataContent(Parameters.build {
                        append("keyword", keywords)
                        append("limit", limit.toString())
                    })
                )
            }
            return handleHttpResponse(response)
        } catch (e: Exception) {
            NetworkResult.Exception(e.message)
        }
    }

    override suspend fun getSongDetail(
        url: String,
        level: String,
        type: String
    ): NetworkResult<NeteaseSong> {
        return try {
            val response = client.post("${baseUrl.value}/Song_V1") {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "url" to url,
                        "level" to level,
                        "type" to type
                    )
                )
            }
            return handleHttpResponse(response)
        } catch (e: Exception) {
            NetworkResult.Exception(e.message)
        }
    }

    override suspend fun getPlaylistDetail(id: String): NetworkResult<NeteasePlaylistResponse> {
        return try {
            val response = client.get("${baseUrl.value}/Playlist?id=$id") {
                contentType(ContentType.Application.Json)
            }
            val handleHttpResponse = handleHttpResponse<NeteasePlaylistResponse>(response)
            return handleHttpResponse
        } catch (e: Exception) {
            NetworkResult.Exception(e.message)
        }
    }

    override suspend fun getAlbumDetail(id: String): NetworkResult<NeteaseAlbumResponse> {
        return try {
            val response = client.get("${baseUrl.value}/Album?id=$id") {
                contentType(ContentType.Application.Json)
            }
            return handleHttpResponse(response)
        } catch (e: Exception) {
            NetworkResult.Exception(e.message)
        }
    }

}
