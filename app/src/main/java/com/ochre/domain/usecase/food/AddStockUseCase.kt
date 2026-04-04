package com.ochre.domain.usecase.food

import com.ochre.domain.repository.FoodRepository

class AddStockUseCase(private val repository: FoodRepository) {
    suspend operator fun invoke(grams: Int) =
        repository.addStock(grams, timestampMillis = System.currentTimeMillis())
}
