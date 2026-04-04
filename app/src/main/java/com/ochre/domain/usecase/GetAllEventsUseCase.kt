package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

class GetAllEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<DogEvent>> = repository.getAllEvents()
}
