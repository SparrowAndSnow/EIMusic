package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.views.components.LibraryPane
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.library_local_subtitle
import eimusic.composeapp.generated.resources.route_local
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LocalView() {
    val localViewModel = koinViewModel<LocalViewModel>()
    val tracks by localViewModel.tracks.collectAsState()

    LibraryPane(
        title = stringResource(Res.string.route_local),
        subtitle = stringResource(Res.string.library_local_subtitle),
        refreshingEnabled = true,
        isLoading = localViewModel.isLoading.value,
        onRefresh = { localViewModel.refreshTracks() },
        tracks = tracks
    )
}
