package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.keyboard.dispatchLoginKey
import com.eimsound.eimusic.keyboard.loginStatusBarHint
import com.eimsound.eimusic.ui.components.DialogPanel
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.widthIn
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Dialog as OverlayDialog
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

/**
 * Login dialog overlay.
 *
 * Manages its own key events and login input state.
 */
@Composable
fun LoginDialog(
    authViewModel: AuthViewModel,
    onGoBack: () -> Unit,
) {
    val authState by authViewModel.authState.bridgeToMutable()
    var loginInput by remember { mutableStateOf("") }
    val safeArea = LocalSafeContentSize.current
    val termWidth = safeArea.columns
    val wrapWidth = (termWidth - 4).coerceAtLeast(20)
    val palette = LocalTuiColors.current
    val statusBarHint = LocalStatusBarHint.current

    LaunchedEffect(Unit) {
        statusBarHint.value = loginStatusBarHint()
    }

    OverlayDialog(onDismissRequest = onGoBack) {
        DialogPanel(title = "Login", onDismiss = onGoBack) {
            Column(
                modifier = Modifier.onKeyEvent { event ->
                    dispatchLoginKey(event, onGoBack)
                },
            ) {
                val s = authState
                val displayName = when (s) {
                    is AuthState.Authenticated -> "Logged in as ${s.user.username}"
                    is AuthState.Loading -> "Logging in..."
                    is AuthState.Error -> "Error: ${s.message}"
                    is AuthState.Unauthenticated -> "Not logged in"
                }
                Text(
                    "  $displayName",
                    modifier = Modifier.widthIn(max = wrapWidth),
                    color = palette.textFg,
                    softWrap = true,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (s !is AuthState.Authenticated) {
                    Text(" ", color = palette.textFg)
                    InputField(
                        value = loginInput,
                        onValueChange = { loginInput = it },
                        placeholder = "Enter username",
                        onConfirm = { authViewModel.login(loginInput) },
                        onCancel = onGoBack,
                    )
                    Text("  Enter to login  |  Esc to cancel", color = palette.textDimFg)
                }
            }
        }
    }
}
