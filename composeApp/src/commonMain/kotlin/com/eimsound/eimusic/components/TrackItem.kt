package com.eimsound.eimusic.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.artists_divider
import eimusic.composeapp.generated.resources.track_menu_add_to_playlist
import eimusic.composeapp.generated.resources.track_menu_download
import eimusic.composeapp.generated.resources.track_menu_favorite
import eimusic.composeapp.generated.resources.track_menu_more_options
import eimusic.composeapp.generated.resources.track_menu_share
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrackItem(
    modifier: Modifier = Modifier,
    track: Track,
    isPlaying: Boolean,
    onPlayClick: () -> Unit = {},
    onTrackNameClick: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onAddToPlaylist: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowIsHovered by rowInteractionSource.collectIsHoveredAsState()
    val menuButtonInteractionSource = remember { MutableInteractionSource() }
    val menuButtonIsHovered by menuButtonInteractionSource.collectIsHoveredAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier.fillMaxWidth()
            .hoverable(rowInteractionSource)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (rowIsHovered) MaterialTheme.colorScheme.surfaceVariant else Color.Unspecified,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackImage(
            modifier = Modifier.zIndex(1f),
            image = track.album?.image,
            isPlaying = isPlaying,
            onPlayClick = onPlayClick
        )

        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            TrackName(modifier = Modifier.zIndex(0f),name = track.name.orEmpty(), onClick = onTrackNameClick)
            ArtistList(
                artists = track.artists.orEmpty(),
                onClick = onArtistClick
            ) {
                Text(stringResource(Res.string.artists_divider), modifier = Modifier.padding(4.dp))
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.hoverable(menuButtonInteractionSource)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.track_menu_more_options),
                    modifier = Modifier.alpha(
                        if (rowIsHovered || menuButtonIsHovered || menuExpanded) 1f else 0f
                    )
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_add_to_playlist)) },
                    onClick = {
                        onAddToPlaylist?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_favorite)) },
                    onClick = {
                        onFavorite?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_download)) },
                    onClick = {
                        onDownload?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_share)) },
                    onClick = {
                        onShare?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    }
                )
            }
        }
    }
}