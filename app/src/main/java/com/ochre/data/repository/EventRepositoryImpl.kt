package com.ochre.data.repository

import com.ochre.data.local.dao.EventDao
import com.ochre.data.local.entity.toDomainModel
import com.ochre.data.local.entity.toEntity
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete implementation of the EventRepository interface from the domain layer.
 * It uses the Room DAO to fetch/save data and maps it to Domain Models before returning it.
 */
class EventRepositoryImpl(
    private val dao: EventDao
) : EventRepository {

    override suspend fun insertEvent(event: DogEvent) {
        dao.insertEvent(event.toEntity())
    }

    override suspend fun updateEvent(event: DogEvent) {
        dao.updateEvent(event.toEntity())
    }

    override suspend fun deleteEvent(event: DogEvent) {
        dao.deleteEvent(event.toEntity())
    }

    override fun getAllEvents(): Flow<List<DogEvent>> {
        return dao.getAllEvents().map { entities ->
            entities.mapNotNull { it.toDomainModel() }
        }
    }

    override fun getEventsByType(type: EventType): Flow<List<DogEvent>> {
        return dao.getEventsByType(type.name).map { entities ->
            entities.mapNotNull { it.toDomainModel() }
        }
    }

    override fun getEventsInRange(from: Long, to: Long): Flow<List<DogEvent>> {
        return dao.getEventsInRange(from, to).map { entities ->
            entities.mapNotNull { it.toDomainModel() }
        }
    }

    override suspend fun getLastEventOfType(type: EventType): DogEvent? {
        return dao.getLastEventOfType(type.name)?.toDomainModel()
    }
}
