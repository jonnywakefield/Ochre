package com.ochre.domain.usecase.walk

import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.repository.WalkRepository
import kotlinx.coroutines.flow.Flow

class GetWalkScheduleUseCase(private val repository: WalkRepository) {
    operator fun invoke(): Flow<WalkScheduleConfig> = repository.getScheduleConfig()
}
