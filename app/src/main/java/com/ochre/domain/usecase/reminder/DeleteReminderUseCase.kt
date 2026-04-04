package com.ochre.domain.usecase.reminder

import com.ochre.domain.repository.ReminderRepository

class DeleteReminderUseCase(private val repository: ReminderRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteReminder(id)
}
