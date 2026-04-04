package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class RemovePooFromWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(walkId: Long, timestampMillis: Long) =
        repository.removePooFromWalk(walkId, timestampMillis)
}
