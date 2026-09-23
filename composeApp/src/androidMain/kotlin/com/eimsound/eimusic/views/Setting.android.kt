package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable

@Composable
actual fun DisplaySettingView(showLoginDialog: Boolean) {
    CommonSettingView(
        showLoginDialog,
        displayTheme = true,
        displayLanguage = false,
        displayProxy = false,
        displayLocalPath = false
    )
}

