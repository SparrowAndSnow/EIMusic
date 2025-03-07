package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class DefaultLayoutViewModel() : ViewModel() {
    var showSideBar by mutableStateOf(false)
        private set

    fun showSideBar(value: Boolean) {
        showSideBar = value
    }
}
