package com.ochre.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object OchreNotificationManager {

    // ── Channels ─────────────────────────────────────────────────────────────
    const val CHANNEL_STATUS    = "ochre_status"    // always-on status (all foreground services share this)
    const val CHANNEL_FOOD      = "ochre_food"      // feed window alerts
    const val CHANNEL_REMINDERS = "ochre_reminders"

    // ── Notification IDs ─────────────────────────────────────────────────────
    const val NOTIF_ID_STATUS = 1000  // single persistent notification

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Always-on summary: walk, food, away"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_FOOD, "Food", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Feed window and meal reminders"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Calendar reminders (vet, treatments, etc)"
            }
        )
    }
}
