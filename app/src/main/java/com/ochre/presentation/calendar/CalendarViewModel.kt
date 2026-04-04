package com.ochre.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.Reminder
import com.ochre.domain.model.WalkSession
import com.ochre.domain.usecase.DeleteEventUseCase
import com.ochre.domain.usecase.GetAllEventsUseCase
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.UpdateEventUseCase
import com.ochre.domain.usecase.alone.DeleteAloneUseCase
import com.ochre.domain.usecase.alone.GetAloneSessionHistoryUseCase
import com.ochre.domain.usecase.reminder.DeleteReminderUseCase
import com.ochre.domain.usecase.walk.DeleteWalkUseCase
import com.ochre.domain.usecase.reminder.GetAllRemindersUseCase
import com.ochre.domain.usecase.reminder.SaveReminderUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class CalendarView { MONTH, WEEK, DAY, TIMELINE }

data class CalendarUiState(
    val view: CalendarView = CalendarView.MONTH,
    val selectedDayMillis: Long = todayStartMillis(),
    val events: List<DogEvent> = emptyList(),
    val walks: List<WalkSession> = emptyList(),
    val aloneSessions: List<AloneSession> = emptyList(),
    val reminders: List<Reminder> = emptyList()
) {
    val dayEnd: (Long) -> Long = { it + 24 * 60 * 60 * 1000L }

    fun eventsForDay(dayStartMillis: Long): List<DogEvent> =
        events.filter { it.timestampMillis in dayStartMillis until dayEnd(dayStartMillis) }

    fun walksForDay(dayStartMillis: Long): List<WalkSession> =
        walks.filter { (it.endMillis ?: it.startMillis) in dayStartMillis until dayEnd(dayStartMillis) }

    fun aloneSessionsForDay(dayStartMillis: Long): List<AloneSession> =
        aloneSessions.filter { (it.endMillis ?: it.startMillis) in dayStartMillis until dayEnd(dayStartMillis) }

    fun remindersForDay(dayStartMillis: Long): List<Reminder> =
        reminders.filter { it.timestampMillis in dayStartMillis until dayEnd(dayStartMillis) }
}

private fun todayStartMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

class CalendarViewModel(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getWalkHistoryUseCase: GetWalkHistoryUseCase,
    private val getAloneSessionHistoryUseCase: GetAloneSessionHistoryUseCase,
    private val logEventUseCase: LogEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val getAllRemindersUseCase: GetAllRemindersUseCase,
    private val saveReminderUseCase: SaveReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val deleteWalkUseCase: DeleteWalkUseCase,
    private val deleteAloneUseCase: DeleteAloneUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(CalendarView.MONTH)
    private val _selectedDay = MutableStateFlow(todayStartMillis())

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_viewState, _selectedDay) { view, day -> Pair(view, day) }
            .combine(getAllEventsUseCase()) { (view, day), events -> Triple(view, day, events) },
        combine(getWalkHistoryUseCase(), getAloneSessionHistoryUseCase()) { walks, alones -> Pair(walks, alones) }
            .combine(getAllRemindersUseCase()) { (walks, alones), reminders -> Triple(walks, alones, reminders) }
    ) { (view, day, events), (walks, alones, reminders) ->
        CalendarUiState(
            view = view,
            selectedDayMillis = day,
            events = events,
            walks = walks.filter { !it.isActive },
            aloneSessions = alones.filter { !it.isActive },
            reminders = reminders
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun setView(view: CalendarView) { _viewState.value = view }
    fun selectDay(millis: Long) { _selectedDay.value = millis }

    fun deleteEvent(event: DogEvent) { viewModelScope.launch { deleteEventUseCase(event) } }

    fun updateEvent(event: DogEvent, timestampMillis: Long, value: Float?, note: String?) {
        viewModelScope.launch { updateEventUseCase(event.copy(timestampMillis = timestampMillis, value = value, note = note)) }
    }

    fun logPastEvent(type: com.ochre.domain.model.EventType, timestampMillis: Long, value: Float?, note: String?) {
        viewModelScope.launch { logEventUseCase(type = type, timestampMillis = timestampMillis, value = value, note = note) }
    }

    fun saveReminder(reminder: Reminder) { viewModelScope.launch { saveReminderUseCase(reminder) } }
    fun deleteReminder(id: Long) { viewModelScope.launch { deleteReminderUseCase(id) } }
    fun deleteWalk(id: Long) { viewModelScope.launch { deleteWalkUseCase(id) } }
    fun deleteAlone(id: Long) { viewModelScope.launch { deleteAloneUseCase(id) } }

    companion object {
        fun provideFactory(
            getAllEventsUseCase: GetAllEventsUseCase,
            getWalkHistoryUseCase: GetWalkHistoryUseCase,
            getAloneSessionHistoryUseCase: GetAloneSessionHistoryUseCase,
            logEventUseCase: LogEventUseCase,
            updateEventUseCase: UpdateEventUseCase,
            deleteEventUseCase: DeleteEventUseCase,
            getAllRemindersUseCase: GetAllRemindersUseCase,
            saveReminderUseCase: SaveReminderUseCase,
            deleteReminderUseCase: DeleteReminderUseCase,
            deleteWalkUseCase: DeleteWalkUseCase,
            deleteAloneUseCase: DeleteAloneUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(
                getAllEventsUseCase, getWalkHistoryUseCase, getAloneSessionHistoryUseCase,
                logEventUseCase, updateEventUseCase, deleteEventUseCase,
                getAllRemindersUseCase, saveReminderUseCase, deleteReminderUseCase,
                deleteWalkUseCase, deleteAloneUseCase
            ) as T
        }
    }
}
