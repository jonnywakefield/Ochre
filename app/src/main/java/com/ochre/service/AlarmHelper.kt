package com.ochre.service

import android.content.Context
import com.ochre.app.OchreApp
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Shared alarm scheduling logic used by OchreApp on cold start and BootReceiver after reboot.
 * All suspend — call from a coroutine.
 */
object AlarmHelper {

    suspend fun scheduleAll(context: Context) {
        val container = (context.applicationContext as OchreApp).container
        val prefs     = NotificationPrefs.get(context)
        val now       = System.currentTimeMillis()

        // ── Food alerts ───────────────────────────────────────────────────────
        if (prefs.foodEnabled) {
            val meals   = container.getMealScheduleUseCase().first()
            val feedLog = container.getFeedLogUseCase().first()
            meals.filter { it.randomReminderEnabled }.forEach { meal ->
                val alreadyFed = feedLog.any { it.isToday() && it.note == "meal:${meal.id}" }
                if (!alreadyFed) {
                    val centerMs     = todayMillisFor(meal.targetHour, meal.targetMinute)
                    val windowOpenMs = centerMs - meal.windowMinutes / 2 * 60_000L
                    val triggerAt    = if (windowOpenMs > now) windowOpenMs else now + 60_000L
                    OchreAlarmScheduler.scheduleFoodAlert(context, triggerAt, meal.id, meal.label)
                }
            }
        }

        // ── Walk gap alert ────────────────────────────────────────────────────
        if (prefs.walkGapEnabled) {
            val activeWalk = container.getActiveWalkUseCase().first()
            if (activeWalk == null) {
                val walkHistory = container.getWalkHistoryUseCase().first()
                val lastWalkEnd = walkHistory.filter { !it.isActive }
                    .maxByOrNull { it.endMillis ?: it.startMillis }?.endMillis
                val triggerAt = if (lastWalkEnd != null)
                    lastWalkEnd + prefs.walkLimitMinutes * 60_000L
                else
                    now + prefs.walkLimitMinutes * 60_000L
                if (triggerAt > now) OchreAlarmScheduler.scheduleWalkGapAlert(context, triggerAt)
            }
        }

        // ── Walk scheduled-time alerts ────────────────────────────────────────
        if (prefs.walkSchedEnabled) {
            val schedule = container.getWalkScheduleUseCase().first()
            schedule.entries.forEach { entry ->
                val triggerAt = nextOccurrenceMillis(entry.targetHour, entry.targetMinute, now)
                OchreAlarmScheduler.scheduleWalkTimeAlert(context, entry.id, triggerAt, entry.label)
            }
        }

        // ── Alone alert ───────────────────────────────────────────────────────
        if (prefs.aloneEnabled) {
            val activeAlone = container.getActiveAloneSessionUseCase().first()
            if (activeAlone != null) {
                val triggerAt = activeAlone.startMillis + prefs.aloneMaxMinutes * 60_000L
                if (triggerAt > now) OchreAlarmScheduler.scheduleAloneAlert(context, triggerAt)
            }
        }
    }

    /**
     * Reschedules all walk scheduled-time alarms for their next occurrence.
     * Called after a walk ends so alarms reflect next-day times where today's has passed.
     */
    suspend fun rescheduleWalkTimeAlerts(context: Context) {
        val container = (context.applicationContext as OchreApp).container
        val prefs = NotificationPrefs.get(context)
        if (!prefs.walkSchedEnabled) return
        val schedule = container.getWalkScheduleUseCase().first()
        val now = System.currentTimeMillis()
        schedule.entries.forEach { entry ->
            val triggerAt = nextOccurrenceMillis(entry.targetHour, entry.targetMinute, now)
            OchreAlarmScheduler.scheduleWalkTimeAlert(context, entry.id, triggerAt, entry.label)
        }
    }

    /**
     * Returns the next occurrence of HH:MM today if it's in the future, otherwise tomorrow.
     */
    fun nextOccurrenceMillis(hour: Int, minute: Int, now: Long): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun todayMillisFor(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun com.ochre.domain.model.DogEvent.isToday(): Boolean {
        val cal = Calendar.getInstance()
        val todayDay  = cal.get(Calendar.DAY_OF_YEAR); val todayYear = cal.get(Calendar.YEAR)
        cal.timeInMillis = timestampMillis
        return cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
    }
}
