package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.BuildConfig
import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.util.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 更新检查 ViewModel
 * 提供跨平台的更新检测功能
 */
class UpdateViewModel(
    private val updateService: UpdateService,
    private val currentVersion: String,
    private val platformType: PlatformType
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState(currentVersion))
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdates()
    }
    /**
     * 检查更新
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChecking = true, error = null)
            when (val result = updateService.checkUpdate(currentVersion, platformType, BuildConfig.OS)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    if (data.hasUpdate && data.update != null) {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = true,
                            updateInfo = data.update,
                            currentVersion = currentVersion,
                            latestVersion = data.latestVersion ?: data.update?.version
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isChecking = false,
                            hasUpdate = false,
                            currentVersion = currentVersion,
                            latestVersion = data.latestVersion ?: currentVersion
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        error = result.message
                    )
                }

                is NetworkResult.Exception -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        error = result.message ?: "检查更新失败"
                    )
                }
            }
        }
    }

    /**
     * 清除更新状态
     */
    fun clearUpdateState() {
        _uiState.value = UpdateUiState(currentVersion)
    }

    /**
     * 标记为已忽略此版本
     */
    fun ignoreVersion(version: String) {
        _uiState.value = _uiState.value.copy(hasUpdate = false)
    }

    fun update() {
        viewModelScope.launch {
            val info = uiState.value.updateInfo ?: return@launch
            if (!updateService.openDownload(info)) {
                _uiState.value = _uiState.value.copy(error = "无法打开下载页面")
            }
        }
    }
}

/**
 * 更新 UI 状态
 */
data class UpdateUiState(
    val currentVersion: String,
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val latestVersion: String? = null,
    val error: String? = null,
)
