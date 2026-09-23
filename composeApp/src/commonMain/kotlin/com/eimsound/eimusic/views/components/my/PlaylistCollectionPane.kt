package com.eimsound.eimusic.views.components.my

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.components.ColumnList
import com.eimsound.eimusic.components.LoadingStateView
import com.eimsound.eimusic.components.MessageStateView
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.theme.LocalMotion
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.library_music_24px
import eimusic.composeapp.generated.resources.loading_text
import eimusic.composeapp.generated.resources.my_empty_description
import eimusic.composeapp.generated.resources.my_empty_title
import eimusic.composeapp.generated.resources.my_track_count
import eimusic.composeapp.generated.resources.my_unknown_playlist
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistCollectionPane(
    mediaLists: List<MediaList>,
    selectedMediaListId: MediaListId?,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    onSelectMediaList: (MediaList) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    if (isLoading && mediaLists.isEmpty()) {
        LoadingStateView(text = stringResource(Res.string.loading_text))
        return
    }

    if (mediaLists.isEmpty()) {
        MessageStateView(
            title = stringResource(Res.string.my_empty_title),
            description = stringResource(Res.string.my_empty_description)
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        ColumnList(
            modifier = Modifier.fillMaxSize(),
            refreshingEnabled = true,
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            list = mediaLists,
            key = MediaList::id,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { playlist ->
            PlaylistCollectionItem(
                mediaList = playlist,
                selected = playlist.id == selectedMediaListId,
                onClick = { onSelectMediaList(playlist) }
            )
        }
    }
}

@Composable
private fun PlaylistCollectionItem(
    mediaList: MediaList,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    val motion = LocalMotion.current

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val hoverProgress by animateFloatAsState(
        targetValue = if (hovered) 1f else 0f,
        animationSpec = motion.fast,
        label = "playlist-hover"
    )
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.medium,
        label = "playlist-selected"
    )

    val idleColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
    val hoverColor = palette.surfaceRaised.copy(alpha = 0.75f)
    val selectedColor = palette.accent.copy(alpha = 0.2f)

    val itemBackground = if (selected) {
        lerp(idleColor, selectedColor, selectedProgress)
    } else {
        lerp(idleColor, hoverColor, hoverProgress)
    }

    val idleBorder = palette.borderSoft.copy(alpha = 0.24f)
    val hoverBorder = palette.borderSoft.copy(alpha = 0.6f)
    val selectedBorder = palette.accent.copy(alpha = 0.56f)
    val itemBorder = if (selected) {
        lerp(idleBorder, selectedBorder, selectedProgress)
    } else {
        lerp(idleBorder, hoverBorder, hoverProgress)
    }

    val (id, tracks, name, creator, image, totalTracks) = when (mediaList) {
        is MediaList.PlaylistMediaList -> mediaList.toUniversalMediaList()
        is MediaList.AlbumMediaList -> mediaList.toUniversalMediaList()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .hoverable(interactionSource)
            .clip(radii.medium)
            .background(itemBackground)
            .border(1.dp, itemBorder, radii.medium)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            if (image.isNullOrBlank()) {
                Icon(
                    painter = painterResource(Res.drawable.library_music_24px),
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = image),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.ifBlank { stringResource(Res.string.my_unknown_playlist) },
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(Res.string.my_track_count, totalTracks),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            val creator = creator.orEmpty()
            if (creator.isNotBlank()) {
                Text(
                    text = creator,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
