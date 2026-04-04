package com.ochre.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.model.WalkSession
import com.ochre.domain.usecase.DeleteEventUseCase
import com.ochre.domain.usecase.GetAllEventsUseCase
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.UpdateEventUseCase
import com.ochre.domain.usecase.alone.DeleteAloneUseCase
import com.ochre.domain.usecase.alone.GetAloneSessionHistoryUseCase
import com.ochre.domain.usecase.walk.DeleteWalkUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HistoryItem {
    abstract val timestampMillis: Long

    data class Event(val event: DogEvent) : HistoryItem() {
        override val timestampMillis: Long get() = event.timestampMillis
    }
    data class Walk(val session: WalkSession) : HistoryItem() {
        override val timestampMillis: Long get() = session.endMillis ?: session.startMillis
    }
    data class Alone(val session: AloneSession) : HistoryItem() {
        override val timestampMillis: Long get() = session.endMillis ?: session.startMillis
    }
}

enum class HistoryFilter { ALL, EVENTS, WALKS, ALONE }

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val activeEventFilter: EventType? = null
) {
    val filtered: List<HistoryItem> get() = when (activeFilter) {
        HistoryFilter.ALL    -> items
        HistoryFilter.EVENTS -> if (activeEventFilter == null)
            items.filterIsInstance<HistoryItem.Event>()
        else
            items.filterIsInstance<HistoryItem.Event>().filter { it.event.type == activeEventFilter }
        HistoryFilter.WALKS  -> items.filterIsInstance<HistoryItem.Walk>()
        HistoryFilter.ALONE  -> items.filterIsInstance<HistoryItem.Alone>()
    }
}

class HistoryViewModel(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getWalkHistoryUseCase: GetWalkHistoryUseCase,
    private val getAloneSessionHistoryUseCase: GetAloneSessionHistoryUseCase,
    private val logEventUseCase: LogEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val deleteWalkUseCase: DeleteWalkUseCase,
    private val deleteAloneUseCase: DeleteAloneUseCase
) : ViewModel() {

    private val activeFilter = MutableStateFlow(HistoryFilter.ALL)
    private val activeEventFilter = MutableStateFlow<EventType?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        getAllEventsUseCase(),
        getWalkHistoryUseCase()
    ) { events, walks -> Pair(events, walks) }
        .combine(getAloneSessionHistoryUseCase()) { (events, walks), alones -> Triple(events, walks, alones) }
        .combine(combine(activeFilter, activeEventFilter) { f, ef -> Pair(f, ef) }) { (events, walks, alones), (filter, eventFilter) ->
            val items: List<HistoryItem> = (
                events.map { HistoryItem.Event(it) } +
                walks.filter { !it.isActive }.map { HistoryItem.Walk(it) } +
                alones.filter { !it.isActive }.map { HistoryItem.Alone(it) }
            ).sortedByDescending { it.timestampMillis }

            HistoryUiState(items = items, activeFilter = filter, activeEventFilter = eventFilter)
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun setFilter(filter: HistoryFilter) {
        activeFilter.value = filter
    }

    fun setEventTypeFilter(type: EventType?) {
        activeEventFilter.value = type
    }

    fun logPastEvent(type: EventType, timestampMillis: Long, value: Float?, note: String?) {
        viewModelScope.launch {
            logEventUseCase(type = type, timestampMillis = timestampMillis, value = value, note = note)
        }
    }

    fun updateEvent(event: DogEvent, timestampMillis: Long, value: Float?, note: String?) {
        viewModelScope.launch {
            updateEventUseCase(event.copy(timestampMillis = timestampMillis, value = value, note = note))
        }
    }

    fun deleteEvent(event: DogEvent) {
        viewModelScope.launch { deleteEventUseCase(event) }
    }

    fun deleteWalk(id: Long) {
        viewModelScope.launch { deleteWalkUseCase(id) }
    }

    fun deleteAlone(id: Long) {
        viewModelScope.launch { deleteAloneUseCase(id) }
    }

    companion object {
        fun provideFactory(
            getAllEventsUseCase: GetAllEventsUseCase,
            getWalkHistoryUseCase: GetWalkHistoryUseCase,
            getAloneSessionHistoryUseCase: GetAloneSessionHistoryUseCase,
            logEventUseCase: LogEventUseCase,
            updateEventUseCase: UpdateEventUseCase,
            deleteEventUseCase: DeleteEventUseCase,
            deleteWalkUseCase: DeleteWalkUseCase,
            deleteAloneUseCase: DeleteAloneUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(
                    getAllEventsUseCase, getWalkHistoryUseCase, getAloneSessionHistoryUseCase,
                    logEventUseCase, updateEventUseCase, deleteEventUseCase,
                    deleteWalkUseCase, deleteAloneUseCase
                ) as T
            }
        }
    }
}
