package com.ochre.data.repository

import com.ochre.data.local.dao.WalkDao
import com.ochre.data.local.entity.*
import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.model.WalkSession
import com.ochre.domain.repository.WalkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WalkRepositoryImpl(private val dao: WalkDao) : WalkRepository {

    override suspend fun startWalk(startMillis: Long): WalkSession {
        val entity = WalkSessionEntity(startMillis = startMillis, endMillis = null)
        val id = dao.insertSession(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun endWalk(walkId: Long, endMillis: Long) {
        val entity = dao.getSessionById(walkId) ?: return
        dao.updateSession(entity.copy(endMillis = endMillis))
    }

    override suspend fun addPooToWalk(walkId: Long, timestampMillis: Long) {
        val entity = dao.getSessionById(walkId) ?: return
        val existing = if (entity.pooEventsJson.isEmpty()) emptyList()
                       else entity.pooEventsJson.split(",")
        dao.updateSession(entity.copy(pooEventsJson = (existing + timestampMillis.toString()).joinToString(",")))
    }

    override suspend fun addPeeToWalk(walkId: Long, timestampMillis: Long) {
        val entity = dao.getSessionById(walkId) ?: return
        val existing = if (entity.peeEventsJson.isEmpty()) emptyList()
                       else entity.peeEventsJson.split(",")
        dao.updateSession(entity.copy(peeEventsJson = (existing + timestampMillis.toString()).joinToString(",")))
    }

    override suspend fun removePooFromWalk(walkId: Long, timestampMillis: Long) {
        val entity = dao.getSessionById(walkId) ?: return
        val updated = entity.pooEventsJson.split(",").filter { it.isNotEmpty() && it.toLong() != timestampMillis }
        dao.updateSession(entity.copy(pooEventsJson = updated.joinToString(",")))
    }

    override suspend fun removePeeFromWalk(walkId: Long, timestampMillis: Long) {
        val entity = dao.getSessionById(walkId) ?: return
        val updated = entity.peeEventsJson.split(",").filter { it.isNotEmpty() && it.toLong() != timestampMillis }
        dao.updateSession(entity.copy(peeEventsJson = updated.joinToString(",")))
    }

    override suspend fun deleteWalk(id: Long) {
        dao.deleteSession(id)
    }

    override suspend fun getWalkById(id: Long): WalkSession? =
        dao.getSessionById(id)?.toDomain()

    override fun getActiveWalk(): Flow<WalkSession?> =
        dao.getActiveSession().map { it?.toDomain() }

    override fun getWalkHistory(): Flow<List<WalkSession>> =
        dao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override fun getScheduleConfig(): Flow<WalkScheduleConfig> =
        combine(
            dao.getConfig(),
            dao.getAllScheduleEntries()
        ) { config, entries ->
            val domainEntries = entries.map { it.toDomain() }
            config?.toDomain(domainEntries) ?: WalkScheduleConfig(entries = domainEntries)
        }

    override suspend fun saveScheduleConfig(config: WalkScheduleConfig) {
        dao.upsertConfig(
            WalkScheduleConfigEntity(
                maxGapMinutes = config.maxGapMinutes,
                quietFromHour = config.quietFromHour,
                quietFromMinute = config.quietFromMinute,
                quietToHour = config.quietToHour,
                quietToMinute = config.quietToMinute
            )
        )
        dao.deleteAllScheduleEntries()
        config.entries.forEach { dao.insertScheduleEntry(it.toEntity()) }
    }
}
