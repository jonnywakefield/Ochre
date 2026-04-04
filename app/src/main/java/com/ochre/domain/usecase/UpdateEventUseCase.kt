package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.repository.EventRepository

class UpdateEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(event: DogEvent) {
        repository.updateEvent(event)
    }
}
