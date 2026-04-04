package com.ochre.app

import android.app.Application
import com.ochre.app.di.AppContainer
import com.ochre.app.di.DefaultAppContainer
import com.ochre.service.OchreNotificationManager

class OchreApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        OchreNotificationManager.createChannels(this)
        // StatusService is started from MainActivity.onStart() to ensure
        // there is a foreground activity context (required on Android 12+)
    }
}
