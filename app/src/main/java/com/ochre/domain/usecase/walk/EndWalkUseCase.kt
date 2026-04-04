package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class EndWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(walkId: Long) =
        repository.endWalk(walkId, endMillis = System.currentTimeMillis())
}
