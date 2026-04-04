package com.ochre.domain.usecase.reminder

import com.ochre.domain.model.Reminder
import com.ochre.domain.repository.ReminderRepository

class SaveReminderUseCase(private val repository: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder): Long = repository.saveReminder(reminder)
}
