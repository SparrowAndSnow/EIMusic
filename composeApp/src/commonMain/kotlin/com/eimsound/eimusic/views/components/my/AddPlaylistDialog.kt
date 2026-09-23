package com.eimsound.eimusic.views.components.my

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.my_add_playlist
import eimusic.composeapp.generated.resources.my_add_playlist_confirm
import eimusic.composeapp.generated.resources.my_add_playlist_hint
import eimusic.composeapp.generated.resources.my_add_playlist_input_label
import eimusic.composeapp.generated.resources.my_add_playlist_loading
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddPlaylistDialog(
    visible: Boolean,
    url: String,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.my_add_playlist))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(Res.string.my_add_playlist_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.my_add_playlist_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text(
                    text = if (isLoading) {
                        stringResource(Res.string.my_add_playlist_loading)
                    } else {
                        stringResource(Res.string.my_add_playlist_confirm)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
