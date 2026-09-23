package com.eimsound.eimusic.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.MessageStateView
import com.eimsound.eimusic.components.TrackList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.util.rememberAutoHideHeaderState
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.library_no_tracks
import eimusic.composeapp.generated.resources.library_no_tracks_desc
import org.jetbrains.compose.resources.stringResource

@Composable
fun LibraryPane(
    title: String,
    subtitle: String,
    refreshingEnabled: Boolean = false,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    tracks: List<Track>,
    chipLabels: List<String> = emptyList(),
) {
    val palette = LocalPalette.current
    val autoHideHeaderState = rememberAutoHideHeaderState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = autoHideHeaderState.isVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ){
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chipLabels.forEach { label ->
                            AssistChip(
                                onClick = {},
                                label = { Text(label) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                    labelColor = palette.textSecondary
                                )
                            )
                        }
                        RefreshButton(onRefresh,!isLoading)
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            MessageStateView(
                title = stringResource(Res.string.library_no_tracks),
                description = stringResource(Res.string.library_no_tracks_desc)
            )
        } else {
            TrackList(
                tracks = tracks,
                refreshingEnabled = refreshingEnabled,
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                autoHideHeaderState = autoHideHeaderState,
                modifier = Modifier.padding(top = 10.dp).weight(1f)
            )
        }
    }
}
