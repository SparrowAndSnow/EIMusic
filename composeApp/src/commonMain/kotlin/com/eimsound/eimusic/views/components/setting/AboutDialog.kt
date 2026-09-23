package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_about_confirm
import eimusic.composeapp.generated.resources.setting_about_copyright
import eimusic.composeapp.generated.resources.setting_about_description
import eimusic.composeapp.generated.resources.setting_about_title
import org.jetbrains.compose.resources.stringResource

/**
 * 关于对话框组件
 */
@Composable
fun AboutDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.setting_about_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.setting_about_description))
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(Res.string.setting_about_copyright))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.setting_about_confirm))
            }
        }
    )
}