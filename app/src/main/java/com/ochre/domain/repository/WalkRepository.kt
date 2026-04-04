package com.ochre.domain.repository

import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.model.WalkSession
import kotlinx.coroutines.flow.Flow

interface WalkRepository {
    suspend fun startWalk(startMillis: Long): WalkSession
    suspend fun endWalk(walkId: Long, endMillis: Long)
    suspend fun addPooToWalk(walkId: Long, timestampMillis: Long)
    suspend fun addPeeToWalk(walkId: Long, timestampMillis: Long)
    suspend fun removePooFromWalk(walkId: Long, timestampMillis: Long)
    suspend fun removePeeFromWalk(walkId: Long, timestampMillis: Long)
    suspend fun deleteWalk(id: Long)
    suspend fun getWalkById(id: Long): WalkSession?
    fun getActiveWalk(): Flow<WalkSession?>
    fun getWalkHistory(): Flow<List<WalkSession>>
    fun getScheduleConfig(): Flow<WalkScheduleConfig>
    suspend fun saveScheduleConfig(config: WalkScheduleConfig)
}
