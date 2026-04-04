package com.ochre.domain.usecase.food

import com.ochre.domain.repository.FoodRepository

class DeleteMealUseCase(private val repository: FoodRepository) {
    suspend operator fun invoke(mealId: Long) = repository.deleteMeal(mealId)
}
