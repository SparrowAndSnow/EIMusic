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
    val paths: List<String> = emptyList()
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
            paths = storage.get(Settings::localPath, emptyList())
        )
    )
    val localPathsState: StateFlow<LocalPathsState> = _localPathsState.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _themeState.value = _themeState.value.copy(darkMode = enabled)
        viewModelScope.launch {
            storage.save(Settings::darkMode, enabled)
        }
    }
    
    fun setThemeFollowSystem(follow: Boolean) {
        _themeState.value = _themeState.value.copy(themeFollowSystem = follow)
        viewModelScope.launch {
            storage.save(Settings::themeFollowSystem, follow)
        }
    }
    
    fun setLanguage(language: String) {
        _languageState.value = _languageState.value.copy(language = language)
        viewModelScope.launch {
            storage.save(Settings::language, language)
        }
    }
    
    // 添加本地路径相关方法
    fun addLocalPath(path: String) {
        val currentPaths = _localPathsState.value.paths
        if (!currentPaths.contains(path)) {
            val newPaths = currentPaths + path
            _localPathsState.value = LocalPathsState(newPaths)
            viewModelScope.launch {
                storage.save(Settings::localPath, newPaths)
            }
        }
    }
    
    fun removeLocalPath(path: String) {
        val currentPaths = _localPathsState.value.paths
        val newPaths = currentPaths.filter { it != path }
        _localPathsState.value = LocalPathsState(newPaths)
        viewModelScope.launch {
            storage.save(Settings::localPath, newPaths)
        }
    }
}