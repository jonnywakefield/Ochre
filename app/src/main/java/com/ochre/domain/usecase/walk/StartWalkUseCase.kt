package com.ochre.domain.usecase.walk

import com.ochre.domain.model.WalkSession
import com.ochre.domain.repository.WalkRepository

class StartWalkUseCase(private val repository: WalkRepository) {
    suspend operator fun invoke(): WalkSession =
        repository.startWalk(startMillis = System.currentTimeMillis())
}
