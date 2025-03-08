package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.layout.SidebarComponent

class DefaultLayoutViewModel : ViewModel() {
    val sideBarState by mutableStateOf(SideBarState())


}

class SideBarState{
    var showSideBar by mutableStateOf(false)
        private set
    var sidebarComponent by mutableStateOf(SidebarComponent.PLAYLIST)
        private set

    fun showSideBar(value: Boolean, component: SidebarComponent) {
        showSideBar = value
        sidebarComponent = component
    }
}