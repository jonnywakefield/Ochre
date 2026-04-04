package com.ochre.domain.usecase.food

import com.ochre.domain.repository.FoodRepository

class LogFeedUseCase(private val repository: FoodRepository) {
    suspend operator fun invoke(grams: Int, mealId: Long? = null) =
        repository.logFeed(
            timestampMillis = System.currentTimeMillis(),
            grams = grams,
            mealId = mealId
        )
}
