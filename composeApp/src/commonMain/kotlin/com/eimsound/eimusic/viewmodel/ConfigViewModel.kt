package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.ToastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigState(
    val serverHttp: String = "",
    val serverWs: String = "",
    val sources: Map<TrackSource, List<String>> = emptyMap()
)

class ConfigViewModel(private val storage: IStorage) : ViewModel() {
    private val _configState = MutableStateFlow(
        storage.get(AppConfig()).let {
            ConfigState(
                serverHttp = it.server.http,
                serverWs = it.server.ws,
                sources = it.sources
            )
        }
    )
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()

    /**
     * 校验并保存 AppConfig。
     * @return true 表示已保存；false 表示校验失败（http/ws 为空或 NETEASE 源为空），未保存。
     */
    fun save(http: String, ws: String, sources: Map<TrackSource, List<String>>): Boolean {
        val trimmedHttp = http.trim()
        val trimmedWs = ws.trim()
        val trimmedSources = sources.mapValues { (_, urls) ->
            urls.map { it.trim() }.filter { it.isNotEmpty() }
        }
        val neteaseUrls = trimmedSources[TrackSource.NETEASE].orEmpty()
        if (trimmedHttp.isEmpty() || trimmedWs.isEmpty() || neteaseUrls.isEmpty()) return false

        val config = AppConfig(
            server = AppConfig.ServerConfig(http = trimmedHttp, ws = trimmedWs),
            sources = trimmedSources
        )
        storage.save(config)
        _configState.value = ConfigState(trimmedHttp, trimmedWs, trimmedSources)
        viewModelScope.launch {
            Toast.show(ToastMessage("配置已保存，重启后生效"))
        }
        return true
    }
}
