package com.ochre.domain.usecase.food

import com.ochre.domain.model.DogEvent
import com.ochre.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow

class GetFeedLogUseCase(private val repository: FoodRepository) {
    operator fun invoke(): Flow<List<DogEvent>> = repository.getFeedLog()
}
