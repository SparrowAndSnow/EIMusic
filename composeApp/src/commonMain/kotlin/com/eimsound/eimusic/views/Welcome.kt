package com.eimsound.eimusic.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.components.TrackList
import com.eimsound.eimusic.viewmodel.WelcomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.loading_text
import eimusic.composeapp.generated.resources.error_text
import eimusic.composeapp.generated.resources.no_tracks_text

@Composable
fun WelcomeView() {
    val viewModel= koinViewModel<WelcomeViewModel>()
    val uiState by viewModel.state.collectAsState()
    val tracks = uiState.tracks

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.loading_text))
            }
        }
        
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.error_text, uiState.error.toString()))
            }
        }
        
        uiState.isEmpty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.no_tracks_text))
            }
        }
        
        else -> {
            TrackList(tracks)
        }
    }
}