package com.eimsound.eimusic.ui.components

import com.eimsound.eimusic.i18n.t

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
        DialogPanel(title = t("trackInfo.title"), onDismiss = onDismiss) {
            Column(Modifier.padding(2)) {
                Text(" " + t("trackInfo.titleLabel") + " " + track.name, color = palette.textFg)
                Text(" " + t("trackInfo.artist") + " " + (track.artists?.joinToString("/") { it.name } ?: "-"), color = palette.textDimFg)
                Text(" " + t("trackInfo.album") + " " + (track.album?.name ?: "-"), color = palette.textDimFg)
                Text(" " + t("trackInfo.source") + " " + track.source.name, color = palette.textDimFg)
                Text(" " + t("trackInfo.id") + " " + track.id.toString(), color = palette.textDimFg)
                Text(" " + t("trackInfo.uri") + " " + (track.uri ?: "-"), color = palette.textDimFg)
                Text(" " + t("hint.help"), color = palette.textDimFg)
            }
        }
    }
}
