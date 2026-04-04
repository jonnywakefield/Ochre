package com.ochre.domain.usecase.food

import com.ochre.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentStockUseCase(private val repository: FoodRepository) {
    operator fun invoke(): Flow<Int> = repository.getCurrentStockGrams()
}
