package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ThemeState(
    val darkMode: Boolean = false,
    val themeFollowSystem: Boolean = false
)

data class LanguageState(
    val language: String = "zh"
)

// 添加本地路径状态数据类
data class LocalPathsState(
    val paths: Set<String> = emptySet()
)

// 添加代理设置状态数据类
data class ProxyState(
    val proxyHost: String? = null,
    val proxyPort: Int = 8080,
    val proxyEnabled: Boolean = false
)

class SettingViewModel(private val storage: Storage) : ViewModel() {
    private val _themeState = MutableStateFlow(
        ThemeState(
            darkMode = storage.get(Settings::darkMode, false),
            themeFollowSystem = storage.get(Settings::themeFollowSystem, false)
        )
    )
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()
    
    private val _languageState = MutableStateFlow(
        LanguageState(
            language = storage.get(Settings::language, "zh")
        )
    )
    val languageState: StateFlow<LanguageState> = _languageState.asStateFlow()

    // 添加本地路径状态
    private val _localPathsState = MutableStateFlow(
        LocalPathsState(
            paths = storage.get(Settings::localPath, emptySet())
        )
    )
    val localPathsState: StateFlow<LocalPathsState> = _localPathsState.asStateFlow()
    
    // 添加代理设置状态
    private val _proxyState = MutableStateFlow(
        ProxyState(
            proxyHost = storage.get(Settings::proxyHost, null),
            proxyPort = storage.get(Settings::proxyPort, 8080),
            proxyEnabled = storage.get(Settings::proxyEnabled, false)
        )
    )
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    fun updateTheme(darkMode: Boolean, themeFollowSystem: Boolean) {
        viewModelScope.launch {
            storage.save(Settings::darkMode, darkMode)
            storage.save(Settings::themeFollowSystem, themeFollowSystem)
            _themeState.value = ThemeState(darkMode, themeFollowSystem)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            storage.save(Settings::language, language)
            _languageState.value = LanguageState(language)
        }
    }

    fun updateLocalPaths(paths: Set<String>) {
        viewModelScope.launch {
            storage.save(Settings::localPath, paths)
            _localPathsState.value = LocalPathsState(paths)
        }
    }
    
    fun updateProxy(proxyHost: String?, proxyPort: Int, proxyEnabled: Boolean) {
        viewModelScope.launch {
            storage.save(Settings::proxyHost, proxyHost)
            storage.save(Settings::proxyPort, proxyPort)
            storage.save(Settings::proxyEnabled, proxyEnabled)
            _proxyState.value = ProxyState(proxyHost, proxyPort, proxyEnabled)
        }
    }
    
    // 恢复被删除的方法
    fun setThemeFollowSystem(followSystem: Boolean) {
        updateTheme(_themeState.value.darkMode, followSystem)
    }

    fun setDarkMode(darkMode: Boolean) {
        updateTheme(darkMode, _themeState.value.themeFollowSystem)
    }

    fun setLanguage(language: String) {
        updateLanguage(language)
    }

    fun addLocalPath(path: String) {
        val currentPaths = _localPathsState.value.paths.toMutableSet()
        if (!currentPaths.contains(path)) {
            currentPaths.add(path)
            updateLocalPaths(currentPaths)
        }
    }

    fun removeLocalPath(path: String) {
        val currentPaths = _localPathsState.value.paths.toMutableSet()
        currentPaths.remove(path)
        updateLocalPaths(currentPaths)
    }
}