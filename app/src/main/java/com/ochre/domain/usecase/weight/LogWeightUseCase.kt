package com.ochre.domain.usecase.weight

import com.ochre.domain.model.WeightEntry
import com.ochre.domain.repository.WeightRepository

class LogWeightUseCase(private val repository: WeightRepository) {
    suspend operator fun invoke(weightKg: Float, timestampMillis: Long, note: String = "") {
        repository.save(WeightEntry(timestampMillis = timestampMillis, weightKg = weightKg, note = note))
    }
}
