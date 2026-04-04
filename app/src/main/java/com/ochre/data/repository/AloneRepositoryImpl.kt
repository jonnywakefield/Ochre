package com.ochre.data.repository

import com.ochre.data.local.dao.AloneDao
import com.ochre.data.local.entity.AloneSessionEntity
import com.ochre.data.local.entity.toDomain
import com.ochre.domain.model.AloneSession
import com.ochre.domain.repository.AloneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AloneRepositoryImpl(private val dao: AloneDao) : AloneRepository {

    override suspend fun startAlone(startMillis: Long): AloneSession {
        val entity = AloneSessionEntity(startMillis = startMillis, endMillis = null)
        val id = dao.insertSession(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun endAlone(sessionId: Long, endMillis: Long) {
        val entity = dao.getSessionById(sessionId) ?: return
        dao.updateSession(entity.copy(endMillis = endMillis))
    }

    override suspend fun deleteAlone(id: Long) {
        dao.deleteSession(id)
    }

    override fun getActiveSession(): Flow<AloneSession?> =
        dao.getActiveSession().map { it?.toDomain() }

    override fun getSessionHistory(): Flow<List<AloneSession>> =
        dao.getAllSessions().map { list -> list.map { it.toDomain() } }
}
