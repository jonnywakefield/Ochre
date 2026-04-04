package com.ochre.service

import android.content.Context

object NotificationPrefs {

    private const val PREFS_NAME = "ochre_notif_prefs"

    const val KEY_ALONE_MAX_MINUTES  = "alone_max_minutes"
    const val KEY_BAR_START_HOUR     = "bar_start_hour"
    const val KEY_BAR_END_HOUR       = "bar_end_hour"

    fun get(context: Context): Prefs {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Prefs(
            aloneMaxMinutes = sp.getInt(KEY_ALONE_MAX_MINUTES, 240),
            barStartHour    = sp.getInt(KEY_BAR_START_HOUR, 6),
            barEndHour      = sp.getInt(KEY_BAR_END_HOUR, 23)
        )
    }

    fun set(context: Context, key: String, value: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }

    data class Prefs(
        val aloneMaxMinutes: Int,
        val barStartHour: Int,
        val barEndHour: Int
    )
}
