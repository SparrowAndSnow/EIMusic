package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.layout.SidebarComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultLayoutViewModel : ViewModel() {
    private val _sideBarState = MutableStateFlow(SideBarState())
    val sideBarState: StateFlow<SideBarState> = _sideBarState.asStateFlow()

    private val _fullScreenPlayerState = MutableStateFlow(FullScreenPlayerState())
    val fullScreenPlayerState: StateFlow<FullScreenPlayerState> = _fullScreenPlayerState.asStateFlow()
    fun updateFullScreenPlayer(isShow: Boolean) {
        _fullScreenPlayerState.value = _fullScreenPlayerState.value.copy(
            isShow = isShow
        )
    }
    fun updateSideBar(show: Boolean, component: SidebarComponent) {
        _sideBarState.value = _sideBarState.value.copy(
            showSideBar = show,
            sidebarComponent = component
        )
    }
}

data class SideBarState(
    val showSideBar: Boolean = false,
    val sidebarComponent: SidebarComponent = SidebarComponent.PLAYLIST
)

data class FullScreenPlayerState(
    val isShow: Boolean = false,
)