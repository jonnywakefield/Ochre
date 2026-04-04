package com.ochre.domain.usecase.alone

import com.ochre.domain.repository.AloneRepository

class DeleteAloneUseCase(private val repository: AloneRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteAlone(id)
}
