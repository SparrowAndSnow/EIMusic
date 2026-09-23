package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.eimsound.eimusic.auth.AuthState
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.account_circle_fill_24px
import eimusic.composeapp.generated.resources.account_info_title
import eimusic.composeapp.generated.resources.account_login_button
import eimusic.composeapp.generated.resources.account_login_hint
import eimusic.composeapp.generated.resources.account_login_welcome
import eimusic.composeapp.generated.resources.account_management
import eimusic.composeapp.generated.resources.account_not_logged_in
import eimusic.composeapp.generated.resources.account_logout
import eimusic.composeapp.generated.resources.account_username_label
import eimusic.composeapp.generated.resources.setting_language_cancel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 登录弹窗组件
 */
@Composable
fun LoginDialog(
    login: (String) -> Unit,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    authState: AuthState
) {
    if (!isVisible) return

    var username by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> {
                errorMessage = ""
                isLoading = true
            }

            is AuthState.Unauthenticated -> {
                errorMessage = ""
                isLoading = false
            }

            is AuthState.Error -> {
                errorMessage = "登录失败"
                isLoading = false
            }

            is AuthState.Authenticated -> {
                onDismiss()
                username = ""
                errorMessage = ""
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            username = ""
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.account_circle_fill_24px),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.account_login_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(Res.string.account_login_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(Res.string.account_username_label)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.account_circle_fill_24px),
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (username.isNotBlank() && !isLoading) {
                                login(username)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && !isLoading) {
                        login(username)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(Res.string.account_login_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    username = ""
                },
                enabled = !isLoading
            ) {
                Text(stringResource(Res.string.setting_language_cancel))
            }
        }
    )
}

/**
 * 账户信息弹窗组件
 */
@Composable
fun AccountInfoDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    authState: AuthState,
    onLogout: () -> Unit,
    onShowLogin: () -> Unit
) {
    if (!isVisible) return

    when (authState) {
        is AuthState.Authenticated -> {
            // 已登录状态 - 显示用户信息和登出选项
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(Res.string.account_info_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.account_circle_fill_24px),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = authState.user.username,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        authState.user.email?.let { email ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(Res.string.account_logout))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.setting_language_cancel))
                    }
                }
            )
        }

        else -> {
            // 未登录状态 - 显示登录提示
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(Res.string.account_management)) },
                text = {
                    Text(stringResource(Res.string.account_not_logged_in))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onShowLogin()
                        }
                    ) {
                        Text(stringResource(Res.string.account_login_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.setting_language_cancel))
                    }
                }
            )
        }
    }
}