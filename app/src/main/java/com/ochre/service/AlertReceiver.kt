package com.ochre.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ochre.R
import com.ochre.app.MainActivity
import com.ochre.app.OchreApp
import com.ochre.service.OchreNotificationManager.CHANNEL_ALERTS
import com.ochre.service.OchreNotificationManager.NOTIF_ID_ALONE_ALERT
import com.ochre.service.OchreNotificationManager.NOTIF_ID_FOOD_ALERT
import com.ochre.service.OchreNotificationManager.NOTIF_ID_WALK_ALERT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FOOD_ALERT      -> handleFoodAlert(context, intent)
            ACTION_WALK_GAP_ALERT  -> handleWalkGapAlert(context)
            ACTION_WALK_SCHED      -> handleWalkSchedAlert(context, intent)
            ACTION_ALONE_ALERT     -> handleAloneAlert(context)
        }
    }

    // ── Food ──────────────────────────────────────────────────────────────────
    // Repeats until fed — regardless of whether the window has passed.

    private fun handleFoodAlert(context: Context, intent: Intent) {
        val prefs     = NotificationPrefs.get(context)
        if (!prefs.foodEnabled) return

        val mealId    = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
        val mealLabel = intent.getStringExtra(EXTRA_MEAL_LABEL) ?: "meal"
        val container = (context.applicationContext as OchreApp).container

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val feedLog = container.getFeedLogUseCase().first()
                val alreadyFed = feedLog.any { it.isToday() && it.note == "meal:$mealId" }
                if (alreadyFed) return@launch   // fed — stop repeating

                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                fire(context, NOTIF_ID_FOOD_ALERT, "$timeStr $mealLabel", "unfed")

                if (prefs.foodRepeatMinutes > 0) {
                    OchreAlarmScheduler.scheduleFoodAlert(
                        context, now + prefs.foodRepeatMinutes * 60_000L, mealId, mealLabel
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ── Walk gap (fires X min after last walk) ────────────────────────────────

    private fun handleWalkGapAlert(context: Context) {
        val prefs = NotificationPrefs.get(context)
        if (!prefs.walkGapEnabled) return

        val container = (context.applicationContext as OchreApp).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeWalk = container.getActiveWalkUseCase().first()
                if (activeWalk != null) return@launch   // walking now — skip

                val now = System.currentTimeMillis()
                val walkHistory = container.getWalkHistoryUseCase().first()
                val lastWalkEnd = walkHistory.filter { !it.isActive }
                    .maxByOrNull { it.endMillis ?: it.startMillis }?.endMillis
                val gapMin = if (lastWalkEnd != null) (now - lastWalkEnd) / 60_000 else Long.MAX_VALUE
                val gapStr = formatMinutes(gapMin)

                fire(context, NOTIF_ID_WALK_ALERT, "no walk $gapStr", "")

                if (prefs.walkGapRepeatMinutes > 0) {
                    OchreAlarmScheduler.scheduleWalkGapAlert(
                        context, now + prefs.walkGapRepeatMinutes * 60_000L
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ── Walk scheduled time ───────────────────────────────────────────────────

    private fun handleWalkSchedAlert(context: Context, intent: Intent) {
        val prefs = NotificationPrefs.get(context)
        if (!prefs.walkSchedEnabled) return

        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)
        val label   = intent.getStringExtra(EXTRA_ENTRY_LABEL) ?: "walk"
        val container = (context.applicationContext as OchreApp).container

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Skip if a walk is active
                val activeWalk = container.getActiveWalkUseCase().first()
                if (activeWalk != null) return@launch

                // Skip if walked within recentMinutes
                val now = System.currentTimeMillis()
                val walkHistory = container.getWalkHistoryUseCase().first()
                val lastWalkEnd = walkHistory.filter { !it.isActive }
                    .maxByOrNull { it.endMillis ?: it.startMillis }?.endMillis
                val minSince = if (lastWalkEnd != null) (now - lastWalkEnd) / 60_000 else Long.MAX_VALUE
                if (minSince < prefs.walkRecentMinutes) return@launch

                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                fire(context, NOTIF_ID_WALK_ALERT, "$timeStr $label", "")
                // Scheduled-time alerts do not self-repeat — they are rescheduled daily by app startup
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ── Alone ─────────────────────────────────────────────────────────────────

    private fun handleAloneAlert(context: Context) {
        val prefs = NotificationPrefs.get(context)
        if (!prefs.aloneEnabled) return

        val container = (context.applicationContext as OchreApp).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeAlone = container.getActiveAloneSessionUseCase().first()
                if (activeAlone == null) return@launch   // back home — stop

                val elapsedMin = (System.currentTimeMillis() - activeAlone.startMillis) / 60_000
                fire(context, NOTIF_ID_ALONE_ALERT, "${formatMinutes(elapsedMin)} alone", "")

                if (prefs.aloneRepeatMinutes > 0) {
                    OchreAlarmScheduler.scheduleAloneAlert(
                        context, System.currentTimeMillis() + prefs.aloneRepeatMinutes * 60_000L
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private fun fire(context: Context, notifId: Int, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(notifId, NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_paw)
            .setColor(0xFFE4A853.toInt())
            .setContentTitle(title)
            .apply { if (text.isNotEmpty()) setContentText(text) }
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        )
    }

    companion object {
        const val ACTION_FOOD_ALERT     = "com.ochre.alert.FOOD"
        const val ACTION_WALK_GAP_ALERT = "com.ochre.alert.WALK_GAP"
        const val ACTION_WALK_SCHED     = "com.ochre.alert.WALK_SCHED"
        const val ACTION_ALONE_ALERT    = "com.ochre.alert.ALONE"

        const val EXTRA_MEAL_ID      = "meal_id"
        const val EXTRA_MEAL_LABEL   = "meal_label"
        const val EXTRA_ENTRY_ID     = "entry_id"
        const val EXTRA_ENTRY_LABEL  = "entry_label"

        fun buildFoodIntent(context: Context, mealId: Long, mealLabel: String) =
            Intent(context, AlertReceiver::class.java).apply {
                action = ACTION_FOOD_ALERT
                putExtra(EXTRA_MEAL_ID, mealId)
                putExtra(EXTRA_MEAL_LABEL, mealLabel)
            }

        fun buildWalkGapIntent(context: Context) =
            Intent(context, AlertReceiver::class.java).apply { action = ACTION_WALK_GAP_ALERT }

        fun buildWalkSchedIntent(context: Context, entryId: Long, label: String) =
            Intent(context, AlertReceiver::class.java).apply {
                action = ACTION_WALK_SCHED
                putExtra(EXTRA_ENTRY_ID, entryId)
                putExtra(EXTRA_ENTRY_LABEL, label)
            }

        fun buildAloneIntent(context: Context) =
            Intent(context, AlertReceiver::class.java).apply { action = ACTION_ALONE_ALERT }
    }
}

private fun formatMinutes(totalMin: Long): String {
    val h = totalMin / 60; val m = totalMin % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}

private fun com.ochre.domain.model.DogEvent.isToday(): Boolean {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR); val todayYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestampMillis
    return cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
}
