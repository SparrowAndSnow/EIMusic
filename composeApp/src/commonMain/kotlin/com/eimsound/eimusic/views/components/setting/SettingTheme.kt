package com.eimsound.eimusic.views.components.setting

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.viewmodel.ThemeState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_appearance
import eimusic.composeapp.generated.resources.setting_dark_mode
import eimusic.composeapp.generated.resources.setting_follow_system
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingTheme(themeState: ThemeState, setThemeFollowSystem: (Boolean) -> Unit, setDarkMode: (Boolean) -> Unit) {
    SettingSection(title = stringResource(Res.string.setting_appearance)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_follow_system)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            trailingContent = {
                Switch(
                    checked = themeState.themeFollowSystem,
                    onCheckedChange = { setThemeFollowSystem(it) }
                )
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_dark_mode)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            trailingContent = {
                Switch(
                    enabled = !themeState.themeFollowSystem,
                    checked = themeState.darkMode,
                    onCheckedChange = { setDarkMode(it) }
                )
            }
        )

    }
}