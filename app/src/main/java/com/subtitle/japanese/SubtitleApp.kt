package com.subtitle.japanese

import android.app.Application

class SubtitleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SubtitleApp
            private set
    }
}
