package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Popup
import com.jakewharton.mosaic.ui.Text

@Composable
fun TrackInfoDialog(track: Track?, onDismiss: () -> Unit) {
    if (track == null) return
    val palette = LocalTuiColors.current
    Popup(modal = true, onDismissRequest = onDismiss) {
        DialogPanel(title = "Track info", onDismiss = onDismiss) {
            Column(Modifier.padding(2)) {
                Text(" Title:   " + track.name, color = palette.textFg)
                Text(" Artist:  " + (track.artists?.joinToString("/") { it.name } ?: "-"), color = palette.textDimFg)
                Text(" Album:   " + (track.album?.name ?: "-"), color = palette.textDimFg)
                Text(" Source:  " + track.source.name, color = palette.textDimFg)
                Text(" Id:      " + track.id.toString(), color = palette.textDimFg)
                Text(" URI:     " + (track.uri ?: "-"), color = palette.textDimFg)
                Text(" Esc:Close", color = palette.textDimFg)
            }
        }
    }
}
