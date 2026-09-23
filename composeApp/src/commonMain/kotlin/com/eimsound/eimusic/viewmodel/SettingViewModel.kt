package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.util.openDirectoryPicker
import io.github.vinceglb.filekit.PlatformFile
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
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 8080,
    val proxyEnabled: Boolean = false
)

data class NotificationState(
    val enabledNotification: Boolean = true,
    val enabledNotificationSound: Boolean = true
)

class SettingViewModel(private val storage: IStorage) : ViewModel() {
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
            proxyHost = storage.get(Settings::proxyHost, "127.0.0.1"),
            proxyPort = storage.get(Settings::proxyPort, 8080),
            proxyEnabled = storage.get(Settings::proxyEnabled, false)
        )
    )
    val proxyState: StateFlow<ProxyState> = _proxyState.asStateFlow()

    private val _notificationState = MutableStateFlow(
        NotificationState(
            enabledNotification = storage.get(Settings::enabledNotification, true),
            enabledNotificationSound = storage.get(Settings::enabledNotificationSound, true),
        )
    )

    val notificationState = _notificationState.asStateFlow()

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

    fun updateProxy(proxyHost: String, proxyPort: Int, proxyEnabled: Boolean) {
        viewModelScope.launch {
            storage.save(Settings::proxyHost, proxyHost)
            storage.save(Settings::proxyPort, proxyPort)
            storage.save(Settings::proxyEnabled, proxyEnabled)
            _proxyState.value = ProxyState(proxyHost, proxyPort, proxyEnabled)
        }
    }

    fun updateNotification(enabledNotification: Boolean, enabledNotificationSound: Boolean) {
        viewModelScope.launch {
            storage.save(Settings::enabledNotification, enabledNotification)
            storage.save(Settings::enabledNotificationSound, enabledNotificationSound)
            _notificationState.value = NotificationState(enabledNotification, enabledNotificationSound)
        }
    }

    fun setNotificationEnabled(enabledNotification: Boolean) {
        updateNotification(enabledNotification, _notificationState.value.enabledNotificationSound)
    }

    fun setNotificationSoundEnabled(enabledNotificationSound: Boolean) {
        updateNotification(_notificationState.value.enabledNotification, enabledNotificationSound)
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

    fun onOpenDirectoryPicker(result: (PlatformFile?) -> Unit) {
        viewModelScope.launch {
            result(openDirectoryPicker())
        }
    }
}