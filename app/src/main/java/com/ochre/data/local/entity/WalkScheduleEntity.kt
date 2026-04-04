package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.model.WalkScheduleEntry

@Entity(tableName = "walk_schedule_entries")
data class WalkScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val targetHour: Int,
    val targetMinute: Int,
    val toleranceMinutes: Int
)

// Walk schedule config stored as a single row (id always 1)
@Entity(tableName = "walk_schedule_config")
data class WalkScheduleConfigEntity(
    @PrimaryKey val id: Int = 1,
    val maxGapMinutes: Int,
    val quietFromHour: Int,
    val quietFromMinute: Int,
    val quietToHour: Int,
    val quietToMinute: Int
)

fun WalkScheduleEntryEntity.toDomain() = WalkScheduleEntry(
    id = id, label = label, targetHour = targetHour,
    targetMinute = targetMinute, toleranceMinutes = toleranceMinutes
)

fun WalkScheduleEntry.toEntity() = WalkScheduleEntryEntity(
    id = id, label = label, targetHour = targetHour,
    targetMinute = targetMinute, toleranceMinutes = toleranceMinutes
)

fun WalkScheduleConfigEntity.toDomain(entries: List<WalkScheduleEntry>) = WalkScheduleConfig(
    entries = entries, maxGapMinutes = maxGapMinutes,
    quietFromHour = quietFromHour, quietFromMinute = quietFromMinute,
    quietToHour = quietToHour, quietToMinute = quietToMinute
)
