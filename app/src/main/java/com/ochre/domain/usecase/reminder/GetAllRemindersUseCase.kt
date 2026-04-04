package com.ochre.domain.usecase.reminder

import com.ochre.domain.model.Reminder
import com.ochre.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class GetAllRemindersUseCase(private val repository: ReminderRepository) {
    operator fun invoke(): Flow<List<Reminder>> = repository.getAllReminders()
}
