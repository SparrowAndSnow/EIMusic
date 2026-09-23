package com.eimsound.eimusic.views.components.my

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.components.LoadingStateView
import com.eimsound.eimusic.components.MessageStateView
import com.eimsound.eimusic.components.TrackList
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.UniversalMediaList
import com.eimsound.eimusic.theme.LocalMotion
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import com.eimsound.eimusic.util.onBackHandler
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.arrow_back_24px
import eimusic.composeapp.generated.resources.delete_24px
import eimusic.composeapp.generated.resources.loading_text
import eimusic.composeapp.generated.resources.my_empty_description
import eimusic.composeapp.generated.resources.my_empty_title
import eimusic.composeapp.generated.resources.my_remove_playlist
import eimusic.composeapp.generated.resources.my_track_count
import eimusic.composeapp.generated.resources.my_tracks_empty
import eimusic.composeapp.generated.resources.my_title
import eimusic.composeapp.generated.resources.my_unknown_playlist
import eimusic.composeapp.generated.resources.queue_music_24px
import eimusic.composeapp.generated.resources.route_my
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistDetailPane(
    mediaList: MediaList?,
    isLoading: Boolean,
    showCompactBack: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    onBackHandler(isBackEnabled = true) {
        onBack()
    }
    val palette = LocalPalette.current
    if (mediaList == null) {
        MessageStateView(
            title = stringResource(Res.string.my_empty_title),
            description = stringResource(Res.string.my_empty_description)
        )
        return
    }

    val universalMediaList = when (mediaList) {
        is MediaList.PlaylistMediaList -> mediaList.toUniversalMediaList()
        is MediaList.AlbumMediaList -> mediaList.toUniversalMediaList()
    }

    val (id, tracks, name, creator, image, totalTracks) = universalMediaList

    if (isLoading) {
        LoadingStateView(text = stringResource(Res.string.loading_text))
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showCompactBack) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_back_24px),
                    contentDescription = null,
                    tint = palette.textSecondary
                )
                Text(
                    text = stringResource(Res.string.my_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
            }
        }

        TrackList(
            modifier = Modifier.fillMaxSize(),
            tracks = tracks,
            refreshingEnabled = true,
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            key = Track::id,
            header = {
                Column {
                    PlaylistHeader(
                        mediaList = universalMediaList,
                        onRemove = onRemove
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = palette.borderSoft.copy(alpha = 0.55f)
                    )
                    if (tracks.isEmpty()) {
                        MessageStateView(
                            title = stringResource(Res.string.my_tracks_empty),
                            description = stringResource(Res.string.my_empty_description)
                        )
                        return@TrackList
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.route_my),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        Text(
                            text = stringResource(Res.string.my_track_count, totalTracks),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun PlaylistHeader(
    mediaList: UniversalMediaList,
    onRemove: () -> Unit,
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    val motion = LocalMotion.current

    val hasImage = !mediaList.image.isNullOrBlank()
    val imageReveal by animateFloatAsState(
        targetValue = if (hasImage) 1f else 0.5f,
        animationSpec = motion.medium,
        label = "my-header-image"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(radii.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(radii.medium)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            if (hasImage) {
                Image(
                    painter = rememberAsyncImagePainter(mediaList.image),
                    contentDescription = mediaList.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            lerp(
                                palette.surfaceRaised,
                                palette.surfaceRaised.copy(alpha = 0.5f),
                                1f - imageReveal
                            )
                        )
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.queue_music_24px),
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaList.name.orEmpty().ifBlank { stringResource(Res.string.my_unknown_playlist) },
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val count = mediaList.totalTracks
            Text(
                text = stringResource(Res.string.my_track_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            val description = mediaList.description.orEmpty().trim()
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(Res.drawable.delete_24px),
                contentDescription = stringResource(Res.string.my_remove_playlist),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
