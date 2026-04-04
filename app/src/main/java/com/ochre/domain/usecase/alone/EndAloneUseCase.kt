package com.ochre.domain.usecase.alone

import com.ochre.domain.repository.AloneRepository

class EndAloneUseCase(private val repository: AloneRepository) {
    suspend operator fun invoke(sessionId: Long) =
        repository.endAlone(sessionId, endMillis = System.currentTimeMillis())
}
