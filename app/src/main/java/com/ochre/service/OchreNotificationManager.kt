package com.ochre.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object OchreNotificationManager {

    // ── Channels ─────────────────────────────────────────────────────────────
    const val CHANNEL_STATUS    = "ochre_status"    // always-on status bar (muted, low priority)
    const val CHANNEL_FOOD      = "ochre_food"      // food window / meal reminder alerts
    const val CHANNEL_REMINDERS = "ochre_reminders" // calendar reminders
    const val CHANNEL_ALERTS    = "ochre_alerts"    // walk / alone / food pop-up alerts (high priority)

    // ── Notification IDs ─────────────────────────────────────────────────────
    const val NOTIF_ID_STATUS       = 1000  // single persistent notification
    const val NOTIF_ID_FOOD_ALERT   = 2001  // food window open / overdue
    const val NOTIF_ID_WALK_ALERT   = 2002  // walk overdue
    const val NOTIF_ID_ALONE_ALERT  = 2003  // dog left alone too long

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
            NotificationChannel(CHANNEL_FOOD, "Food reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Feed window open and meal reminders"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Calendar reminders (vet, treatments, etc)"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Walk overdue, alone limit, feed window alerts — shown as heads-up popups"
                enableVibration(true)
                setShowBadge(true)
            }
        )
    }
}
