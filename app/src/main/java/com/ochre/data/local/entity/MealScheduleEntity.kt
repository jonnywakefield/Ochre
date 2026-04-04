package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.MealScheduleEntry

@Entity(tableName = "meal_schedule")
data class MealScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val targetHour: Int,
    val targetMinute: Int,
    val windowMinutes: Int,
    val defaultGrams: Int,
    val varyAmount: Boolean,
    val minGrams: Int,
    val maxGrams: Int,
    val randomReminderEnabled: Boolean
)

fun MealScheduleEntity.toDomain() = MealScheduleEntry(
    id = id, label = label, targetHour = targetHour, targetMinute = targetMinute,
    windowMinutes = windowMinutes, defaultGrams = defaultGrams, varyAmount = varyAmount,
    minGrams = minGrams, maxGrams = maxGrams, randomReminderEnabled = randomReminderEnabled
)

fun MealScheduleEntry.toEntity() = MealScheduleEntity(
    id = id, label = label, targetHour = targetHour, targetMinute = targetMinute,
    windowMinutes = windowMinutes, defaultGrams = defaultGrams, varyAmount = varyAmount,
    minGrams = minGrams, maxGrams = maxGrams, randomReminderEnabled = randomReminderEnabled
)
