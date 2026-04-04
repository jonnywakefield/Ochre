package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType

/**
 * The Data layer's representation of an Event, specifically mapped to a SQLite table.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // Stored as String so it's resilient to Enum changes
    val timestampMillis: Long,
    val value: Float?,
    val note: String?
)

// Extension functions to map between Domain (DogEvent) and Data (EventEntity) models
fun EventEntity.toDomainModel(): DogEvent? {
    val type = enumValues<EventType>().firstOrNull { it.name == eventType } ?: return null
    return DogEvent(
        id = id,
        type = type,
        timestampMillis = timestampMillis,
        value = value,
        note = note
    )
}

fun DogEvent.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        eventType = type.name,
        timestampMillis = timestampMillis,
        value = value,
        note = note
    )
}
