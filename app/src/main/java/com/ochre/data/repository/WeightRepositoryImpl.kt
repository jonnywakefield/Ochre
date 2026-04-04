package com.ochre.data.repository

import com.ochre.data.local.dao.WeightDao
import com.ochre.data.local.entity.WeightEntity
import com.ochre.domain.model.WeightEntry
import com.ochre.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeightRepositoryImpl(private val dao: WeightDao) : WeightRepository {

    override fun getAll(): Flow<List<WeightEntry>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(entry: WeightEntry) {
        dao.insert(WeightEntity(id = entry.id, timestampMillis = entry.timestampMillis, weightKg = entry.weightKg, note = entry.note))
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    private fun WeightEntity.toDomain() = WeightEntry(id = id, timestampMillis = timestampMillis, weightKg = weightKg, note = note)
}
