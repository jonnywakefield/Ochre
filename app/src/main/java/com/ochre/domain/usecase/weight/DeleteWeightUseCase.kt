package com.ochre.domain.usecase.weight

import com.ochre.domain.repository.WeightRepository

class DeleteWeightUseCase(private val repository: WeightRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
