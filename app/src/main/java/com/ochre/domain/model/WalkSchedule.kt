package com.ochre.domain.model

data class WalkScheduleEntry(
    val id: Long = 0,
    val label: String,           // e.g. "Morning"
    val targetHour: Int,
    val targetMinute: Int,
    val toleranceMinutes: Int = 30
)

data class WalkScheduleConfig(
    val entries: List<WalkScheduleEntry> = emptyList(),
    val maxGapMinutes: Int = 360,       // 6 hours
    val quietFromHour: Int = 22,
    val quietFromMinute: Int = 0,
    val quietToHour: Int = 7,
    val quietToMinute: Int = 0
)
