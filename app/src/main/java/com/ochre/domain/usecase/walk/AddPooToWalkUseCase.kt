package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class AddPooToWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(walkId: Long) =
        repository.addPooToWalk(walkId, timestampMillis = System.currentTimeMillis())
}
