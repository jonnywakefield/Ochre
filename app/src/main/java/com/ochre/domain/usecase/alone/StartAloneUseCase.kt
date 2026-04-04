package com.ochre.domain.usecase.alone

import com.ochre.domain.model.AloneSession
import com.ochre.domain.repository.AloneRepository

class StartAloneUseCase(private val repository: AloneRepository) {
    suspend operator fun invoke(): AloneSession =
        repository.startAlone(startMillis = System.currentTimeMillis())
}
