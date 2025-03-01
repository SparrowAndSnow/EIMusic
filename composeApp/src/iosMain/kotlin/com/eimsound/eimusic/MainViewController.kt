package com.eimsound.eimusic

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }



actual fun navigationLayoutType(): NavigationSuiteType = NavigationSuiteType.NavigationDrawer