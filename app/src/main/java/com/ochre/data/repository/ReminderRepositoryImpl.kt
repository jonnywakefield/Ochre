package com.ochre.data.repository

import com.ochre.data.local.dao.ReminderDao
import com.ochre.data.local.entity.toDomain
import com.ochre.data.local.entity.toEntity
import com.ochre.domain.model.Reminder
import com.ochre.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(private val dao: ReminderDao) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getRemindersInRange(fromMillis: Long, toMillis: Long): Flow<List<Reminder>> =
        dao.getInRange(fromMillis, toMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun saveReminder(reminder: Reminder): Long =
        dao.upsert(reminder.toEntity())

    override suspend fun deleteReminder(id: Long) = dao.delete(id)
}
