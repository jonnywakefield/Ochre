package com.ochre.domain.repository

import com.ochre.domain.model.AloneSession
import kotlinx.coroutines.flow.Flow

interface AloneRepository {
    suspend fun startAlone(startMillis: Long): AloneSession
    suspend fun endAlone(sessionId: Long, endMillis: Long)
    suspend fun deleteAlone(id: Long)
    fun getActiveSession(): Flow<AloneSession?>
    fun getSessionHistory(): Flow<List<AloneSession>>
}
