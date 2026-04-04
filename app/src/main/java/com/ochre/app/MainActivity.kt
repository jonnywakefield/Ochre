package com.ochre.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ochre.presentation.common.OchreTheme
import com.ochre.presentation.navigation.AppNavGraph
import com.ochre.service.StatusService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as OchreApp).container
        setContent {
            OchreTheme {
                AppNavGraph(container = appContainer)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start status service from a foreground activity context —
        // required on Android 12+ to avoid BackgroundServiceStartNotAllowedException
        startForegroundService(Intent(this, StatusService::class.java))
    }
}
