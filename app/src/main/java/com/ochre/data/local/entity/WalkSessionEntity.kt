package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.WalkSession

@Entity(tableName = "walk_sessions")
data class WalkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long?,
    val pooEventsJson: String = "",
    val peeEventsJson: String = ""
)

private fun String.toMillisList(): List<Long> =
    if (isEmpty()) emptyList() else split(",").map { it.toLong() }

fun WalkSessionEntity.toDomain(): WalkSession = WalkSession(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    pooEvents = pooEventsJson.toMillisList(),
    peeEvents = peeEventsJson.toMillisList()
)

fun WalkSession.toEntity(): WalkSessionEntity = WalkSessionEntity(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    pooEventsJson = pooEvents.joinToString(","),
    peeEventsJson = peeEvents.joinToString(",")
)
