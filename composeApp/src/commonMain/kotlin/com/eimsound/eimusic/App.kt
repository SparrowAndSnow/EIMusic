package com.eimsound.eimusic

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.eimsound.eimusic.components.Navigation
import com.eimsound.eimusic.theme.EIMusicTheme

@Composable
@Preview
fun App() {
    EIMusicTheme {
        Navigation(navigationLayoutType())
    }
}


expect fun navigationLayoutType(): NavigationSuiteType
