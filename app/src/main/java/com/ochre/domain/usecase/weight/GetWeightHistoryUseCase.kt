package com.ochre.domain.usecase.weight

import com.ochre.domain.model.WeightEntry
import com.ochre.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow

class GetWeightHistoryUseCase(private val repository: WeightRepository) {
    operator fun invoke(): Flow<List<WeightEntry>> = repository.getAll()
}
