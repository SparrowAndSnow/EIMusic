package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.person_fill_24px
import eimusic.composeapp.generated.resources.setting_account
import eimusic.composeapp.generated.resources.setting_connected
import eimusic.composeapp.generated.resources.setting_tap_to_sign_in
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun SettingAccount(authState: AuthState, login: (String) -> Unit, logout: () -> Unit, showLoginDialog: Boolean) {
    var showAccountDialog by remember { mutableStateOf(false) }
    var showLoginDialogInner by remember { mutableStateOf(false) }

    LaunchedEffect(showLoginDialog) {
        showLoginDialogInner = showLoginDialog
    }
    SettingSection(title = stringResource(Res.string.setting_account)) {
        ListItem(
            headlineContent = {
                Text(
                    text = when (val state = authState) {
                        is AuthState.Authenticated -> state.user.username
                        else -> stringResource(Res.string.setting_account)
                    }
                )
            },
            supportingContent = {
                Text(
                    if (authState is AuthState.Authenticated) stringResource(Res.string.setting_connected) else stringResource(
                        Res.string.setting_tap_to_sign_in
                    )
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.person_fill_24px), contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.clickable {
                if (authState is AuthState.Authenticated) showAccountDialog = true else showLoginDialogInner = true
            }
        )
    }

    LoginDialog(
        login = login,
        isVisible = showLoginDialogInner,
        onDismiss = { showLoginDialogInner = false },
        authState = authState
    )
    AccountInfoDialog(
        isVisible = showAccountDialog,
        onDismiss = { showAccountDialog = false },
        authState = authState,
        onLogout = logout,
        onShowLogin = { showLoginDialogInner = true })
}