package com.eimsound.eimusic.views.components.my

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.displayName
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.my_no_local_playlists
import eimusic.composeapp.generated.resources.my_select_playlist
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalPlaylistPickerDialog(
    visible: Boolean,
    playlists: List<MediaList>,
    onDismiss: () -> Unit,
    onSelect: (MediaList) -> Unit,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.my_select_playlist)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(Res.string.my_no_local_playlists))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists, key = { it.id.toString() }) { playlist ->
                        Text(
                            text = playlist.displayName(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(playlist) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
