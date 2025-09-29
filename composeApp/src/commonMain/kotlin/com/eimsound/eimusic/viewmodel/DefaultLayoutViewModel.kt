package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.layout.SidebarComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultLayoutViewModel : ViewModel() {
    private val _sideBarState = MutableStateFlow(SideBarState())
    val sideBarState: StateFlow<SideBarState> = _sideBarState.asStateFlow()

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