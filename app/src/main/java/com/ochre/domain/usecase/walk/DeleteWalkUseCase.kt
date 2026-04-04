package com.ochre.domain.usecase.walk

import com.ochre.domain.repository.WalkRepository

class DeleteWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteWalk(id)
}
