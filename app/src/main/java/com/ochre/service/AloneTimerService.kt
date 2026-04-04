package com.ochre.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ochre.R
import com.ochre.service.OchreNotificationManager.CHANNEL_STATUS
import com.ochre.service.OchreNotificationManager.NOTIF_ID_STATUS

class AloneTimerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Attach to the shared status notification so Android's foreground
        // service requirement is satisfied. StatusService owns the content.
        startForeground(
            NOTIF_ID_STATUS,
            NotificationCompat.Builder(this, CHANNEL_STATUS)
                .setSmallIcon(R.drawable.ic_dog_alone)
                .setContentTitle("Away…")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
