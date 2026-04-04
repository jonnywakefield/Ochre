package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.Reminder

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestampMillis: Long,
    val notifyBeforeMinutes: Int
)

fun ReminderEntity.toDomain() = Reminder(id = id, title = title, timestampMillis = timestampMillis, notifyBeforeMinutes = notifyBeforeMinutes)
fun Reminder.toEntity() = ReminderEntity(id = id, title = title, timestampMillis = timestampMillis, notifyBeforeMinutes = notifyBeforeMinutes)
