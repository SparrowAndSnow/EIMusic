package com.eimsound.eimusic

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

object AppStateProvider : DefaultLifecycleObserver {
    var isAppInForeground = false

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) { isAppInForeground = true }
    override fun onStop(owner: LifecycleOwner) { isAppInForeground = false }
}