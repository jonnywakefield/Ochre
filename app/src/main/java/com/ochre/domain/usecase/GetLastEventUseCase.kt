package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.repository.EventRepository

/**
 * Use Case to retrieve the most recent event of a specific type.
 */
class GetLastEventUseCase(
    private val repository: EventRepository
) {
    suspend operator fun invoke(type: EventType): DogEvent? {
        return repository.getLastEventOfType(type)
    }
}
