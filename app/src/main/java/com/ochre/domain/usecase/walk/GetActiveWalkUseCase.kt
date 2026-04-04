package com.ochre.domain.usecase.walk

import com.ochre.domain.model.WalkSession
import com.ochre.domain.repository.WalkRepository
import kotlinx.coroutines.flow.Flow

class GetActiveWalkUseCase(private val repository: WalkRepository) {
    operator fun invoke(): Flow<WalkSession?> = repository.getActiveWalk()
}
