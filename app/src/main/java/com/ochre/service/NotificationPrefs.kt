package com.ochre.service

import android.content.Context

object NotificationPrefs {

    private const val PREFS_NAME = "ochre_notif_prefs"

    const val KEY_ALONE_MAX_MINUTES   = "alone_max_minutes"
    const val KEY_BAR_START_MINUTE    = "bar_start_minute"
    const val KEY_BAR_END_MINUTE      = "bar_end_minute"

    fun get(context: Context): Prefs {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Prefs(
            aloneMaxMinutes  = sp.getInt(KEY_ALONE_MAX_MINUTES, 240),
            barStartMinute   = sp.getInt(KEY_BAR_START_MINUTE, 6 * 60),   // 06:00 default
            barEndMinute     = sp.getInt(KEY_BAR_END_MINUTE,   23 * 60)   // 23:00 default
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
        val barStartMinute: Int,
        val barEndMinute: Int
    )
}
