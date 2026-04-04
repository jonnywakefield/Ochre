package com.ochre.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ochre.domain.model.AloneSession

@Entity(tableName = "alone_sessions")
data class AloneSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long?
)

fun AloneSessionEntity.toDomain() = AloneSession(id = id, startMillis = startMillis, endMillis = endMillis)
fun AloneSession.toEntity() = AloneSessionEntity(id = id, startMillis = startMillis, endMillis = endMillis)
