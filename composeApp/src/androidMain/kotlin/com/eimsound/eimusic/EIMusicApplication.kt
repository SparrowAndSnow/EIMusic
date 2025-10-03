package com.eimsound.eimusic

import android.app.Application

class EIMusicApplication : Application() {
    companion object {
        lateinit var instance: EIMusicApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}