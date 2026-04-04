package com.ochre.domain.usecase

import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLastEventPerTypeUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<Map<EventType, DogEvent?>> {
        return repository.getAllEvents().map { events ->
            EventType.entries.associateWith { type ->
                events.firstOrNull { it.type == type }
            }
        }
    }
}
