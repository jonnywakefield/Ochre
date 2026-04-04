package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class RemovePeeFromWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(walkId: Long, timestampMillis: Long) =
        repository.removePeeFromWalk(walkId, timestampMillis)
}
