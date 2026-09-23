package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.eimsound.eimusic.components.LoadingStateView
import com.eimsound.eimusic.viewmodel.SearchResultViewModel
import com.eimsound.eimusic.views.components.LibraryPane
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchResultView(query: String) {
    val searchResultViewModel = koinViewModel<SearchResultViewModel>()
    val state = searchResultViewModel.state.collectAsState()
    LaunchedEffect(query){
        searchResultViewModel.search(query)
    }
    when(state.value.loading){
        true -> LoadingStateView("Loading...")
        false -> LibraryPane(
            title = "SearchResult Collection",
            subtitle = "SearchResult for \"$query\"",
            tracks = state.value.tracks,
        )
    }
}