package com.eimsound.eimusic.views.components.my

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.create
import eimusic.composeapp.generated.resources.my_create_local
import eimusic.composeapp.generated.resources.my_local_name
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateLocalPlaylistDialog(
    visible: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.my_create_local)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(Res.string.my_local_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
