package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.info_fill_24px
import eimusic.composeapp.generated.resources.setting_about
import eimusic.composeapp.generated.resources.setting_about_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingAbout() {
    var showAboutDialog by remember { mutableStateOf(false) }
    SettingSection(title = stringResource(Res.string.setting_about_title)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_about)) },
            leadingContent = { Icon(painterResource(Res.drawable.info_fill_24px), contentDescription = null) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.clickable { showAboutDialog = true }
        )
    }
    AboutDialog(isVisible = showAboutDialog, onDismiss = { showAboutDialog = false })
}