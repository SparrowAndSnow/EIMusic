package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.layout.SidebarComponent
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultLayoutViewModel(
    private val storage: IStorage
) : ViewModel() {
    private val _sideBarState = MutableStateFlow(SideBarState())
    val sideBarState: StateFlow<SideBarState> = _sideBarState.asStateFlow()

    private val _fullScreenPlayerState = MutableStateFlow(FullScreenPlayerState())
    val fullScreenPlayerState: StateFlow<FullScreenPlayerState> = _fullScreenPlayerState.asStateFlow()

    private val _navigationDrawerState = MutableStateFlow(
        NavigationDrawerState(
            isExpanded = storage.get(Settings::navDrawerExpanded, true)
        )
    )
    val navigationDrawerState: StateFlow<NavigationDrawerState> = _navigationDrawerState.asStateFlow()

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

    fun toggleDrawer() {
        setDrawerExpanded(!_navigationDrawerState.value.isExpanded)
    }

    fun setDrawerExpanded(value: Boolean) {
        _navigationDrawerState.value = NavigationDrawerState(isExpanded = value)
        storage.save(Settings::navDrawerExpanded, value)
    }
}

data class SideBarState(
    val showSideBar: Boolean = false,
    val sidebarComponent: SidebarComponent = SidebarComponent.PLAYLIST
)

data class FullScreenPlayerState(
    val isShow: Boolean = false,
)

data class NavigationDrawerState(
    val isExpanded: Boolean = true
)
