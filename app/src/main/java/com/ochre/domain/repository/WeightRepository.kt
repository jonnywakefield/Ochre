package com.ochre.domain.repository

import com.ochre.domain.model.WeightEntry
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun getAll(): Flow<List<WeightEntry>>
    suspend fun save(entry: WeightEntry)
    suspend fun delete(id: Long)
}
