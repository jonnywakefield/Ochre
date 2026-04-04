package com.ochre.domain.usecase.walk

import com.ochre.domain.model.WalkSession
import com.ochre.domain.repository.WalkRepository
import kotlinx.coroutines.flow.Flow

class GetWalkHistoryUseCase(private val repository: WalkRepository) {
    operator fun invoke(): Flow<List<WalkSession>> = repository.getWalkHistory()
}
