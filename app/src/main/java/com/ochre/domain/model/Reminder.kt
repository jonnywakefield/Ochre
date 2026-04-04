package com.ochre.domain.model

data class Reminder(
    val id: Long = 0,
    val title: String,
    val timestampMillis: Long,
    val notifyBeforeMinutes: Int = 0  // 0 = notify at exact time
)
