package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class AddPeeToWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(walkId: Long) =
        repository.addPeeToWalk(walkId, timestampMillis = System.currentTimeMillis())
}
