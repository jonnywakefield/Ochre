package com.ochre.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Central scheduler for all heads-up alert alarms.
 *
 * Request codes:
 *  100 — food alert  (single alarm; replaced per meal via extras)
 *  101 — walk gap alert (fires after X min since last walk)
 *  102 — alone alert
 *  200..299 — walk schedule-time alerts (one per WalkScheduleEntry, keyed by entryId % 100)
 */
object OchreAlarmScheduler {

    private const val RC_FOOD_ALERT       = 100
    private const val RC_WALK_GAP_ALERT   = 101
    private const val RC_ALONE_ALERT      = 102
    private const val RC_WALK_SCHED_BASE  = 200   // + (entryId % 100)

    // ── Food ─────────────────────────────────────────────────────────────────

    fun scheduleFoodAlert(context: Context, triggerAtMillis: Long, mealId: Long, mealLabel: String) {
        val intent = AlertReceiver.buildFoodIntent(context, mealId, mealLabel)
        schedule(context, RC_FOOD_ALERT, triggerAtMillis, intent)
    }

    fun cancelFoodAlert(context: Context) {
        cancel(context, RC_FOOD_ALERT, AlertReceiver.buildFoodIntent(context, 0L, ""))
    }

    // ── Walk gap (fires X min after last walk end) ────────────────────────────

    fun scheduleWalkGapAlert(context: Context, triggerAtMillis: Long) {
        schedule(context, RC_WALK_GAP_ALERT, triggerAtMillis, AlertReceiver.buildWalkGapIntent(context))
    }

    fun cancelWalkGapAlert(context: Context) {
        cancel(context, RC_WALK_GAP_ALERT, AlertReceiver.buildWalkGapIntent(context))
    }

    // ── Walk scheduled time (one alarm per WalkScheduleEntry) ─────────────────

    fun scheduleWalkTimeAlert(context: Context, entryId: Long, triggerAtMillis: Long, label: String) {
        val rc = RC_WALK_SCHED_BASE + (entryId % 100).toInt()
        schedule(context, rc, triggerAtMillis, AlertReceiver.buildWalkSchedIntent(context, entryId, label))
    }

    fun cancelWalkTimeAlert(context: Context, entryId: Long) {
        val rc = RC_WALK_SCHED_BASE + (entryId % 100).toInt()
        cancel(context, rc, AlertReceiver.buildWalkSchedIntent(context, entryId, ""))
    }

    fun cancelAllWalkTimeAlerts(context: Context, entryIds: List<Long>) {
        entryIds.forEach { cancelWalkTimeAlert(context, it) }
    }

    // ── Alone ─────────────────────────────────────────────────────────────────

    fun scheduleAloneAlert(context: Context, triggerAtMillis: Long) {
        schedule(context, RC_ALONE_ALERT, triggerAtMillis, AlertReceiver.buildAloneIntent(context))
    }

    fun cancelAloneAlert(context: Context) {
        cancel(context, RC_ALONE_ALERT, AlertReceiver.buildAloneIntent(context))
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun schedule(context: Context, requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancel(context: Context, requestCode: Int, intent: Intent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        pi.cancel()
    }
}
