package com.eimsound.eimusic.views.components.setting

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.viewmodel.NotificationState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_notification
import eimusic.composeapp.generated.resources.setting_notification_enabled
import eimusic.composeapp.generated.resources.setting_notification_sound_enabled
import org.jetbrains.compose.resources.stringResource


@Composable
fun SettingNotification(
    notificationState: NotificationState,
    setEnabledNotification: (Boolean) -> Unit,
    setEnabledNotificationSound: (Boolean) -> Unit,
) {
    SettingSection(title = stringResource(Res.string.setting_notification)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_notification_enabled)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            trailingContent = {
                Switch(
                    checked = notificationState.enabledNotification,
                    onCheckedChange = { setEnabledNotification(it) }
                )
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_notification_sound_enabled)) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            trailingContent = {
                Switch(
                    enabled = notificationState.enabledNotification,
                    checked = notificationState.enabledNotificationSound,
                    onCheckedChange = { setEnabledNotificationSound(it) }
                )
            }
        )
    }
}