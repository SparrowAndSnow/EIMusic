package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.folder_fill_24px
import eimusic.composeapp.generated.resources.ok
import eimusic.composeapp.generated.resources.setting_add_local_path
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddLocalPathDialog(
    isVisible: Boolean,
    newPath: String,
    onDirectoryPickerClick: () -> Unit,
    onNewPathChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.setting_add_local_path)) },
        text = {
            OutlinedTextField(
                value = newPath,
                onValueChange = onNewPathChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onDirectoryPickerClick) {
                        Icon(painterResource(Res.drawable.folder_fill_24px), contentDescription = "选择文件夹")
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )

}