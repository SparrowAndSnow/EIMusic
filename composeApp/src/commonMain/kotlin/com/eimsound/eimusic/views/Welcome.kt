package com.eimsound.eimusic.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.LoadingStateView
import com.eimsound.eimusic.components.MessageStateView
import com.eimsound.eimusic.components.TrackList
import com.eimsound.eimusic.views.components.home.HomeHeroCard
import com.eimsound.eimusic.views.components.home.HomeSection
import com.eimsound.eimusic.util.rememberAutoHideHeaderState
import com.eimsound.eimusic.viewmodel.WelcomeViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.error_text
import eimusic.composeapp.generated.resources.loading_text
import eimusic.composeapp.generated.resources.no_tracks_text
import eimusic.composeapp.generated.resources.welcome_daily_spotlight
import eimusic.composeapp.generated.resources.welcome_daily_spotlight_desc
import eimusic.composeapp.generated.resources.welcome_no_music_yet
import eimusic.composeapp.generated.resources.welcome_sync_failed
import eimusic.composeapp.generated.resources.welcome_trending_now
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeView() {
    val viewModel = koinViewModel<WelcomeViewModel>()
    val uiState by viewModel.state.collectAsState()
    val autoHideHeaderState = rememberAutoHideHeaderState()
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = autoHideHeaderState.isVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HomeHeroCard(
                    title = stringResource(Res.string.welcome_daily_spotlight),
                    subtitle = stringResource(Res.string.welcome_daily_spotlight_desc),
                    onRefresh = {
                        viewModel.loadTopTracks()
                    },
                    isLoading = uiState.isLoading,
                )
                HomeSection(
                    title = stringResource(Res.string.welcome_trending_now),
                    caption = "${uiState.tracks.size} tracks"
                )
            }
        }

        when {
            uiState.isLoading -> {
                LoadingStateView(stringResource(Res.string.loading_text))
            }

            uiState.error != null -> {
                MessageStateView(
                    title = stringResource(Res.string.welcome_sync_failed),
                    description = stringResource(Res.string.error_text, uiState.error.toString())
                )
            }

            uiState.isEmpty -> {
                MessageStateView(
                    title = stringResource(Res.string.welcome_no_music_yet),
                    description = stringResource(Res.string.no_tracks_text)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TrackList(
                        autoHideHeaderState = autoHideHeaderState, tracks = uiState.tracks,
                        refreshingEnabled = true, isRefreshing = uiState.isLoading, onRefresh = viewModel::loadTopTracks)
                }
            }
        }
    }

}
