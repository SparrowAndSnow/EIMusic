package com.eimsound.eimusic.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_title
import eimusic.composeapp.generated.resources.setting_account
import eimusic.composeapp.generated.resources.setting_dark_mode
import eimusic.composeapp.generated.resources.setting_dark_mode_desc
import eimusic.composeapp.generated.resources.setting_follow_system
import eimusic.composeapp.generated.resources.setting_follow_system_desc
import eimusic.composeapp.generated.resources.setting_about
import eimusic.composeapp.generated.resources.setting_about_title
import eimusic.composeapp.generated.resources.setting_about_description
import eimusic.composeapp.generated.resources.setting_about_copyright
import eimusic.composeapp.generated.resources.setting_about_confirm
import eimusic.composeapp.generated.resources.setting_language
import eimusic.composeapp.generated.resources.setting_language_desc
import eimusic.composeapp.generated.resources.setting_language_cancel
import eimusic.composeapp.generated.resources.setting_local_paths
import eimusic.composeapp.generated.resources.setting_local_paths_desc
import eimusic.composeapp.generated.resources.setting_add_local_path
import eimusic.composeapp.generated.resources.setting_remove_local_path
import eimusic.composeapp.generated.resources.setting_proxy
import eimusic.composeapp.generated.resources.setting_proxy_desc
import eimusic.composeapp.generated.resources.setting_proxy_host
import eimusic.composeapp.generated.resources.setting_proxy_port
import eimusic.composeapp.generated.resources.setting_proxy_enable
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState
import com.eimsound.eimusic.viewmodel.SettingViewModel
import org.jetbrains.compose.resources.stringResource

@Serializable
object SettingRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingView() {
    val settingViewModel = koinViewModel<SettingViewModel>()
    val themeState by settingViewModel.themeState.collectAsState()
    val languageState by settingViewModel.languageState.collectAsState()
    // 获取本地路径状态
    val localPathsState by settingViewModel.localPathsState.collectAsState()
    // 获取代理设置状态
    val proxyState by settingViewModel.proxyState.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(languageState.language) }
    // 添加路径相关状态
    var showAddPathDialog by remember { mutableStateOf(false) }
    var newPath by remember { mutableStateOf("") }
    // 代理设置相关状态
    var showProxyDialog by remember { mutableStateOf(false) }
    var proxyHost by remember { mutableStateOf(proxyState.proxyHost ?: "") }
    var proxyPort by remember { mutableStateOf(proxyState.proxyPort.toString()) }
    var proxyEnabled by remember { mutableStateOf(proxyState.proxyEnabled) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(Res.string.setting_title),
            modifier = Modifier.padding(8.dp)
        )

        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_account)) },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.clickable {
                // TODO: 跳转到登录界面
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_follow_system)) },
            supportingContent = { Text(stringResource(Res.string.setting_follow_system_desc)) },
            trailingContent = {
                Switch(
                    checked = themeState.themeFollowSystem,
                    onCheckedChange = {
                        settingViewModel.setThemeFollowSystem(it)
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_dark_mode)) },
            supportingContent = { Text(stringResource(Res.string.setting_dark_mode_desc)) },
            trailingContent = {
                Switch(
                    enabled = !themeState.themeFollowSystem,
                    checked = themeState.darkMode,
                    onCheckedChange = {
                        settingViewModel.setDarkMode(it)
                    }
                )
            }
        )

        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_language)) },
            supportingContent = { Text(stringResource(Res.string.setting_language_desc)) },
            trailingContent = {
                Text(
                    text = when (languageState.language) {
                        "zh" -> "中文"
                        "en" -> "English"
                        else -> "Unknown"
                    },
                )
            },
            modifier = Modifier.clickable {
                selectedLanguage = languageState.language
                showLanguageDialog = true
            }
        )

        // 代理设置
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_proxy)) },
            supportingContent = { Text(stringResource(Res.string.setting_proxy_desc)) },
            trailingContent = {
                Switch(
                    checked = proxyState.proxyEnabled,
                    onCheckedChange = { enabled ->
                        proxyEnabled = enabled
                        settingViewModel.updateProxy(
                            if (proxyHost.isNotBlank()) proxyHost else null,
                            proxyPort.toIntOrNull() ?: 8080,
                            enabled
                        )
                    }
                )
            },
            modifier = Modifier.clickable {
                proxyHost = proxyState.proxyHost ?: ""
                proxyPort = proxyState.proxyPort.toString()
                proxyEnabled = proxyState.proxyEnabled
                showProxyDialog = true
            }
        )

        // 本地音乐路径设置
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_local_paths)) },
            supportingContent = { Text(stringResource(Res.string.setting_local_paths_desc)) },
            trailingContent = {
                IconButton(
                    onClick = {
                        newPath = ""
                        showAddPathDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.setting_add_local_path),
                    )
                }
            }
        )

        // 显示已添加的路径列表
        localPathsState.paths.forEach { path ->
            ListItem(
                headlineContent = {
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = { settingViewModel.removeLocalPath(path) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.setting_remove_local_path)
                        )
                    }
                },
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // 关于部分
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_about)) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.clickable { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(Res.string.setting_about_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.setting_about_description))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.setting_about_copyright))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(Res.string.setting_about_confirm))
                }
            }
        )
    }

    if (showLanguageDialog) {
        val languages = mapOf(
            "zh" to "中文",
            "en" to "English"
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(Res.string.setting_language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    languages.forEach { (code, name) ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedLanguage == code)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedLanguage == code,
                                    onClick = {
                                        selectedLanguage = code
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    selectedLanguage = code
                                }
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingViewModel.setLanguage(selectedLanguage)
                        showLanguageDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.setting_about_confirm),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.setting_language_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // 添加路径对话框
    if (showAddPathDialog) {
        AlertDialog(
            onDismissRequest = { showAddPathDialog = false },
            title = { Text(stringResource(Res.string.setting_add_local_path)) },
            text = {
                OutlinedTextField(
                    value = newPath,
                    onValueChange = { newPath = it },
                    label = { Text(stringResource(Res.string.setting_local_paths)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPath.isNotBlank()) {
                            settingViewModel.addLocalPath(newPath)
                        }
                        showAddPathDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.setting_about_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPathDialog = false }) {
                    Text(stringResource(Res.string.setting_language_cancel))
                }
            }
        )
    }

    // 代理设置对话框
    if (showProxyDialog) {
        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = { Text(stringResource(Res.string.setting_proxy)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = proxyHost,
                        onValueChange = { proxyHost = it },
                        label = { Text(stringResource(Res.string.setting_proxy_host)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = proxyPort,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                                proxyPort = it
                            }
                        },
                        label = { Text(stringResource(Res.string.setting_proxy_port)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.setting_proxy_enable),
                            modifier = Modifier.alignByBaseline()
                        )
                        
                        Switch(
                            checked = proxyEnabled,
                            onCheckedChange = { proxyEnabled = it },
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val host = if (proxyHost.isNotBlank()) proxyHost else null
                        val port = proxyPort.toIntOrNull() ?: 8080
                        settingViewModel.updateProxy(host, port, proxyEnabled)
                        showProxyDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.setting_about_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProxyDialog = false }) {
                    Text(stringResource(Res.string.setting_language_cancel))
                }
            }
        )
    }
}