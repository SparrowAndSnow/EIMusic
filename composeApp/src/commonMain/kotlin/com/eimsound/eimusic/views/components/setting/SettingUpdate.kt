package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.AppVersion
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.viewmodel.UpdateViewModel
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.info_fill_24px
import eimusic.composeapp.generated.resources.setting_update_check
import eimusic.composeapp.generated.resources.setting_update_check_button
import eimusic.composeapp.generated.resources.setting_update_current_version
import eimusic.composeapp.generated.resources.setting_update_download_error
import eimusic.composeapp.generated.resources.setting_update_error
import eimusic.composeapp.generated.resources.setting_update_new_version_found
import eimusic.composeapp.generated.resources.setting_update_no_update
import eimusic.composeapp.generated.resources.setting_update_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingUpdate(
    updateViewModel: UpdateViewModel,
    onCheckUpdate: () -> Unit = {},
) {
    val uiState by updateViewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    SettingSection(title = stringResource(Res.string.setting_update_title)) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(Res.string.setting_update_check),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = "${stringResource(Res.string.setting_update_current_version)}：${uiState.currentVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.info_fill_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                if (uiState.isChecking) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.setting_update_check_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            updateViewModel.checkForUpdates()
                            onCheckUpdate()
                        }
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.clickable {
                if (!uiState.isChecking) {
                    updateViewModel.checkForUpdates()
                    onCheckUpdate()
                }
            }
        )

        // 显示错误信息
        uiState.error?.let { error ->
            ListItem(
                headlineContent = {
                    Text(
                        text = "${stringResource(Res.string.setting_update_error)}: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                )
            )
        }

        // 显示有新版本
        if (uiState.hasUpdate && uiState.updateInfo != null) {
            val updateInfo = uiState.updateInfo!!
            ListItem(
                headlineContent = {
                    Text(
                        text = "${stringResource(Res.string.setting_update_new_version_found)}: ${updateInfo.version}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = updateInfo.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        UpdateExecuteState(uiState.downloadState)
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                modifier = Modifier.clickable {
                    showUpdateDialog = true
                }
            )
        } else {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.setting_update_no_update),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            )
        }
    }

    val updateInfo = uiState.updateInfo
    if (updateInfo != null && showUpdateDialog) {
        UpdateDialog(
            updateInfo = updateInfo,
            currentVersion = AppVersion.VERSION_NAME,
            onDismiss = { showUpdateDialog = false },
            onUpdate = {
                updateViewModel.update()
                showUpdateDialog = false
            },
            onLater = {
                updateViewModel.ignoreVersion(uiState.latestVersion ?: AppVersion.VERSION_NAME)
                updateViewModel.clearUpdateState()
                showUpdateDialog = false
            }
        )
    }
}

@Composable
fun UpdateExecuteState(downloadState: DownloadState) {
    val palette = LocalPalette.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadProgressColor by remember { mutableStateOf(palette.accent) }

    when (downloadState) {
        is DownloadState.None -> {
            return
        }

        is DownloadState.Progress -> {
            downloadProgress = downloadState.percent.toFloat() / 100
            downloadProgressColor = palette.accent
        }

        is DownloadState.Error -> {
            Text(
                text = "${stringResource(Res.string.setting_update_download_error)}: ${downloadState.cause.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.error
            )
            return
        }

        is DownloadState.Success -> {
            downloadProgress = 1f
            downloadProgressColor = palette.success
        }

        is DownloadState.Loading -> {
            downloadProgress = 0f
            downloadProgressColor = palette.accent
        }
    }

    LinearProgressIndicator(progress = {
        downloadProgress
    }, color = downloadProgressColor)
}