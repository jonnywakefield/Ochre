package com.ochre.domain.repository

import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining how the Domain Layer interacts with Event Data.
 * The Domain Layer doesn't know about Room or SQLite, it only knows about DogEvents.
 */
interface EventRepository {
    suspend fun insertEvent(event: DogEvent)
    suspend fun updateEvent(event: DogEvent)
    suspend fun deleteEvent(event: DogEvent)

    fun getAllEvents(): Flow<List<DogEvent>>
    fun getEventsByType(type: EventType): Flow<List<DogEvent>>
    fun getEventsInRange(from: Long, to: Long): Flow<List<DogEvent>>

    suspend fun getLastEventOfType(type: EventType): DogEvent?
}
