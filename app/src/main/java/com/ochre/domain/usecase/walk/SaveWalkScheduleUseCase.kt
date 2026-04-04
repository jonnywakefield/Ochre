package com.ochre.domain.usecase.walk

import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.repository.WalkRepository

class SaveWalkScheduleUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(config: WalkScheduleConfig) =
        repository.saveScheduleConfig(config)
}
