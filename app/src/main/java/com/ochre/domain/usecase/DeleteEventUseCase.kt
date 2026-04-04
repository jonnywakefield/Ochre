package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.repository.EventRepository

class DeleteEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(event: DogEvent) {
        repository.deleteEvent(event)
    }
}
