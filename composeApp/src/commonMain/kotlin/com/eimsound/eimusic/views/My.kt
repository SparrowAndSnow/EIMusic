package com.eimsound.eimusic.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import com.eimsound.eimusic.viewmodel.MyViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.views.components.RefreshButton
import com.eimsound.eimusic.views.components.my.AddPlaylistDialog
import com.eimsound.eimusic.views.components.my.MyLibraryScaffold
import com.eimsound.eimusic.views.components.my.PlaylistCollectionPane
import com.eimsound.eimusic.views.components.my.PlaylistDetailPane
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.add_24px
import eimusic.composeapp.generated.resources.my_add_playlist
import eimusic.composeapp.generated.resources.my_error_title
import eimusic.composeapp.generated.resources.my_playlist_count
import eimusic.composeapp.generated.resources.my_subtitle
import eimusic.composeapp.generated.resources.my_title
import eimusic.composeapp.generated.resources.ok
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyView() {
    val myViewModel = koinViewModel<MyViewModel>()
    val playingListViewModel = koinViewModel<PlayingListViewModel>()
    val state by myViewModel.state.collectAsState()
    val playingListState by playingListViewModel.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var playlistUrl by rememberSaveable { mutableStateOf("") }
    var showCompactDetail by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.selectedMediaList?.id) {
        if (state.selectedMediaList == null) {
            showCompactDetail = false
        }
    }

    AddPlaylistDialog(
        visible = showAddDialog,
        url = playlistUrl,
        isLoading = state.isLoading,
        onUrlChange = { playlistUrl = it },
        onDismiss = {
            showAddDialog = false
            playlistUrl = ""
        },
        onConfirm = {
            if (playlistUrl.isNotBlank()) {
                myViewModel.add(playlistUrl.trim())
                playlistUrl = ""
                showAddDialog = false
            }
        }
    )

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { myViewModel.clearError() },
            title = { Text(stringResource(Res.string.my_error_title)) },
            text = { Text(state.error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { myViewModel.clearError() }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MyLibraryScaffold(
            modifier = Modifier.weight(1f),
            isCompactDetailVisible = showCompactDetail && state.selectedMediaList != null,
            collectionPane = {
                Column {
                    MyHeroSection(
                        playlistCount = state.playlists.size,
                        onAddPlaylist = { showAddDialog = true },
                        onRefresh = myViewModel::load,
                        isLoading = state.isLoading
                    )
                    PlaylistCollectionPane(
                        mediaLists = state.playlists,
                        selectedMediaListId = state.selectedMediaList?.id,
                        isLoading = state.isLoading,
                        onRefresh = myViewModel::load,
                        onSelectMediaList = { mediaList ->
                            myViewModel.selectMediaList(mediaList)
                            showCompactDetail = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

            },
            detailPane = { showCompactBack ->
                PlaylistDetailPane(
                    mediaList = state.selectedMediaList,
                    isLoading = state.isLoading,
                    showCompactBack = showCompactBack,
                    onBack = { showCompactDetail = false },
                    onRemove = {
                        state.selectedMediaList?.let { playlist ->
                            myViewModel.remove(playlist)
                        }
                    },
                    onRefresh = {
                        val mediaList = state.selectedMediaList
                        if (mediaList != null) myViewModel.refreshMediaList(mediaList)
                    }
                )
            }
        )
    }
}

@Composable
private fun MyHeroSection(
    playlistCount: Int,
    onAddPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean = false
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = radii.medium,
        color = palette.surfaceGlass,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.my_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary
                )
                Text(
                    text = stringResource(Res.string.my_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.my_playlist_count, playlistCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,

                    )
                    RefreshButton(onRefresh, !isLoading)
                }
            }

            FilledTonalButton(
                onClick = onAddPlaylist,
                shape = radii.chip,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.my_add_playlist),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
