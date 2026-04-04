package com.ochre.domain.repository

import com.ochre.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getRemindersInRange(fromMillis: Long, toMillis: Long): Flow<List<Reminder>>
    suspend fun saveReminder(reminder: Reminder): Long
    suspend fun deleteReminder(id: Long)
}
