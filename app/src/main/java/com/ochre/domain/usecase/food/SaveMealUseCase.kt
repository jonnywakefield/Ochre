package com.ochre.domain.usecase.food

import com.ochre.domain.model.MealScheduleEntry
import com.ochre.domain.repository.FoodRepository

class SaveMealUseCase(private val repository: FoodRepository) {
    suspend operator fun invoke(entry: MealScheduleEntry) = repository.saveMeal(entry)
}
