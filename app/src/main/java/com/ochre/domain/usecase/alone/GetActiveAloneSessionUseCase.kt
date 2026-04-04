package com.ochre.domain.usecase.alone

import com.ochre.domain.model.AloneSession
import com.ochre.domain.repository.AloneRepository
import kotlinx.coroutines.flow.Flow

class GetActiveAloneSessionUseCase(private val repository: AloneRepository) {
    operator fun invoke(): Flow<AloneSession?> = repository.getActiveSession()
}
