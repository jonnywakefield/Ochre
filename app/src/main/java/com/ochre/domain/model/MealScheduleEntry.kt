package com.ochre.domain.model

data class MealScheduleEntry(
    val id: Long = 0,
    val label: String,               // e.g. "Morning"
    val targetHour: Int,
    val targetMinute: Int,
    val windowMinutes: Int = 60,     // total window width (±half on each side, or open window)
    val defaultGrams: Int = 200,
    val varyAmount: Boolean = false,
    val minGrams: Int = 180,
    val maxGrams: Int = 220,
    val randomReminderEnabled: Boolean = true
)
