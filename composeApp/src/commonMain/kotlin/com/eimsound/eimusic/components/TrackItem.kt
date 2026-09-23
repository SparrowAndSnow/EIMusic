package com.eimsound.eimusic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.theme.LocalPalette
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.artists_divider
import eimusic.composeapp.generated.resources.delete_24px
import eimusic.composeapp.generated.resources.download_24px
import eimusic.composeapp.generated.resources.favorite_fill_24px
import eimusic.composeapp.generated.resources.more_vert_24px
import eimusic.composeapp.generated.resources.playlist_add_24px
import eimusic.composeapp.generated.resources.share_24px
import eimusic.composeapp.generated.resources.skip_next_fill_24px
import eimusic.composeapp.generated.resources.track_menu_add_to_playlist
import eimusic.composeapp.generated.resources.track_menu_add_to_playlist_next
import eimusic.composeapp.generated.resources.track_menu_download
import eimusic.composeapp.generated.resources.track_menu_favorite
import eimusic.composeapp.generated.resources.track_menu_more_options
import eimusic.composeapp.generated.resources.track_menu_remove_from_playlist
import eimusic.composeapp.generated.resources.track_menu_share
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrackItem(
    modifier: Modifier = Modifier,
    track: Track,
    isPlaying: Boolean,
    showAddToQueue: Boolean = true,
    showRemoveFromQueue: Boolean = false,
    index: Int? = null,
    onPlayClick: () -> Unit = {},
    onTrackNameClick: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onAddToPlaylist: ((Track, AddTrackStrategy) -> Unit)? = null,
    onRemoveFromPlaylist: ((Track) -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val palette = LocalPalette.current
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowIsHovered by rowInteractionSource.collectIsHoveredAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(rowInteractionSource)
            .background(
                color = if (rowIsHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = index?.toString() ?: "",
            color = if (isPlaying) palette.accent else palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.size(20.dp)
        )

        TrackImage(
            modifier = Modifier
                .size(56.dp),
            image = track.album?.image,
            isPlaying = isPlaying,
            onPlayClick = onPlayClick
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlaying) palette.accent else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onTrackNameClick)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                track.artists.orEmpty().forEachIndexed { artistIndex, artist ->
                    Text(
                        text = artist.name.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { onArtistClick(artist) }
                            .padding(vertical = 2.dp)
                    )
                    if (artistIndex != track.artists.orEmpty().lastIndex) {
                        Text(
                            text = stringResource(Res.string.artists_divider),
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = track.duration?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.alpha(if (rowIsHovered || isPlaying) 1f else 0.72f)
        )

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(Res.drawable.more_vert_24px),
                    contentDescription = stringResource(Res.string.track_menu_more_options),
                    tint = if (rowIsHovered || menuExpanded) palette.textPrimary else palette.textSecondary
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (showAddToQueue) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.track_menu_add_to_playlist)) },
                        onClick = {
                            onAddToPlaylist?.invoke(track, AddTrackStrategy.AddToLast)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(painterResource(Res.drawable.playlist_add_24px), contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.track_menu_add_to_playlist_next)) },
                        onClick = {
                            onAddToPlaylist?.invoke(track, AddTrackStrategy.AddToNext)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(painterResource(Res.drawable.skip_next_fill_24px), contentDescription = null) }
                    )
                }
                if(showRemoveFromQueue){
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.track_menu_remove_from_playlist)) },
                        onClick = {
                            onRemoveFromPlaylist?.invoke(track)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(painterResource(Res.drawable.delete_24px), contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_favorite)) },
                    onClick = {
                        onFavorite?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(painterResource(Res.drawable.favorite_fill_24px), contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_download)) },
                    onClick = {
                        onDownload?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(painterResource(Res.drawable.download_24px), contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.track_menu_share)) },
                    onClick = {
                        onShare?.invoke()
                        menuExpanded = false
                    },
                    leadingIcon = { Icon(painterResource(Res.drawable.share_24px), contentDescription = null) }
                )
            }
        }
    }
}
