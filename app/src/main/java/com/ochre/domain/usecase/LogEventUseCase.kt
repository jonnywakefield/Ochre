package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.repository.EventRepository

/**
 * Use Case for logging any type of event.
 * Follows the Single Responsibility Principle: its only job is to create an event and save it.
 */
class LogEventUseCase(
    private val repository: EventRepository
) {
    suspend operator fun invoke(
        type: EventType,
        timestampMillis: Long = System.currentTimeMillis(),
        value: Float? = null,
        note: String? = null
    ) {
        val event = DogEvent(
            type = type,
            timestampMillis = timestampMillis,
            value = value,
            note = note
        )
        repository.insertEvent(event)
    }
}
