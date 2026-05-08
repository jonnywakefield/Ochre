package com.ochre.service

import android.content.Context

object NotificationPrefs {

    private const val PREFS_NAME = "ochre_notif_prefs"

    // Status bar
    const val KEY_BAR_START_MINUTE         = "bar_start_minute"
    const val KEY_BAR_END_MINUTE           = "bar_end_minute"

    // Alone alert
    const val KEY_ALONE_ENABLED            = "alone_enabled"
    const val KEY_ALONE_MAX_MINUTES        = "alone_max_minutes"
    const val KEY_ALONE_REPEAT_MINUTES     = "alone_repeat_minutes"   // 0 = no repeat

    // Food alert
    const val KEY_FOOD_ENABLED             = "food_enabled"
    const val KEY_FOOD_REPEAT_MINUTES      = "food_repeat_minutes"    // 0 = no repeat

    // Walk gap alert (fires X min after last walk ended)
    const val KEY_WALK_GAP_ENABLED         = "walk_gap_enabled"
    const val KEY_WALK_LIMIT_MINUTES       = "walk_limit_minutes"
    const val KEY_WALK_GAP_REPEAT_MINUTES  = "walk_gap_repeat_minutes"  // 0 = no repeat

    // Walk scheduled-time alerts (one per WalkScheduleEntry)
    const val KEY_WALK_SCHED_ENABLED       = "walk_sched_enabled"
    const val KEY_WALK_RECENT_MINUTES      = "walk_recent_minutes"    // skip if walked within this many min

    fun get(context: Context): Prefs {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Prefs(
            barStartMinute       = sp.getInt(KEY_BAR_START_MINUTE,        6 * 60),
            barEndMinute         = sp.getInt(KEY_BAR_END_MINUTE,          23 * 60),
            aloneEnabled         = sp.getBoolean(KEY_ALONE_ENABLED,       true),
            aloneMaxMinutes      = sp.getInt(KEY_ALONE_MAX_MINUTES,       240),
            aloneRepeatMinutes   = sp.getInt(KEY_ALONE_REPEAT_MINUTES,    30),
            foodEnabled          = sp.getBoolean(KEY_FOOD_ENABLED,        true),
            foodRepeatMinutes    = sp.getInt(KEY_FOOD_REPEAT_MINUTES,     15),
            walkGapEnabled       = sp.getBoolean(KEY_WALK_GAP_ENABLED,    true),
            walkLimitMinutes     = sp.getInt(KEY_WALK_LIMIT_MINUTES,      360),
            walkGapRepeatMinutes = sp.getInt(KEY_WALK_GAP_REPEAT_MINUTES, 30),
            walkSchedEnabled     = sp.getBoolean(KEY_WALK_SCHED_ENABLED,  true),
            walkRecentMinutes    = sp.getInt(KEY_WALK_RECENT_MINUTES,     60)
        )
    }

    fun set(context: Context, key: String, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(key, value).apply()
    }

    fun set(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    data class Prefs(
        val barStartMinute: Int,
        val barEndMinute: Int,
        // Alone
        val aloneEnabled: Boolean,
        val aloneMaxMinutes: Int,
        val aloneRepeatMinutes: Int,
        // Food
        val foodEnabled: Boolean,
        val foodRepeatMinutes: Int,
        // Walk gap
        val walkGapEnabled: Boolean,
        val walkLimitMinutes: Int,
        val walkGapRepeatMinutes: Int,
        // Walk scheduled times
        val walkSchedEnabled: Boolean,
        val walkRecentMinutes: Int    // skip scheduled walk alert if walked within this many minutes
    )
}
