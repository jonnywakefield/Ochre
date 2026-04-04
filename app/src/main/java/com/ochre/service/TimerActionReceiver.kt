package com.ochre.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ochre.R
import com.ochre.app.OchreApp
import com.ochre.service.OchreNotificationManager.CHANNEL_STATUS
import com.ochre.service.OchreNotificationManager.NOTIF_ID_STATUS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WALK_POO = "com.ochre.action.WALK_POO"
        const val ACTION_WALK_PEE = "com.ochre.action.WALK_PEE"
        const val ACTION_WALK_END = "com.ochre.action.WALK_END"
        const val ACTION_ALONE_END = "com.ochre.action.ALONE_END"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as OchreApp).container
        when (intent.action) {
            ACTION_WALK_POO -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let {
                    container.addPooToWalkUseCase(it.id)
                    flashConfirmation(context, "Poo recorded")
                }
            }
            ACTION_WALK_PEE -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let {
                    container.addPeeToWalkUseCase(it.id)
                    flashConfirmation(context, "Pee recorded")
                }
            }
            ACTION_WALK_END -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let {
                    container.endWalkUseCase(it.id)
                    context.stopService(Intent(context, WalkTimerService::class.java))
                }
            }
            ACTION_ALONE_END -> scope.launch {
                val active = container.getActiveAloneSessionUseCase().first()
                active?.let {
                    container.endAloneUseCase(it.id)
                    context.stopService(Intent(context, AloneTimerService::class.java))
                }
            }
        }
    }

    /** Briefly updates the status notification title to confirm the recorded event, then lets StatusService resume. */
    private suspend fun flashConfirmation(context: Context, label: String) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val msg = "$label · ${timeFmt.format(Date())}"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_dog)
            .setContentTitle(msg)
            .setContentText("Walking…")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        nm.notify(NOTIF_ID_STATUS, notif)
        delay(3_000)
        // StatusService ticker (10s) will overwrite naturally; nothing more needed
    }
}
