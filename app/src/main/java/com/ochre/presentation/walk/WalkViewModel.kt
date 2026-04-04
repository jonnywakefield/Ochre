package com.ochre.presentation.walk

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.model.WalkSession
import com.ochre.domain.model.EventType
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.walk.AddPeeToWalkUseCase
import com.ochre.domain.usecase.walk.AddPooToWalkUseCase
import com.ochre.domain.usecase.walk.EndWalkUseCase
import com.ochre.domain.usecase.walk.GetActiveWalkUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import com.ochre.domain.usecase.walk.GetWalkScheduleUseCase
import com.ochre.domain.usecase.walk.RemovePeeFromWalkUseCase
import com.ochre.domain.usecase.walk.RemovePooFromWalkUseCase
import com.ochre.domain.usecase.walk.SaveWalkScheduleUseCase
import com.ochre.domain.usecase.walk.StartWalkUseCase
import com.ochre.service.WalkTimerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalkUiState(
    val activeWalk: WalkSession? = null,
    val history: List<WalkSession> = emptyList(),
    val schedule: WalkScheduleConfig = WalkScheduleConfig()
) {
    val isWalkActive: Boolean get() = activeWalk != null
}

class WalkViewModel(
    private val startWalkUseCase: StartWalkUseCase,
    private val endWalkUseCase: EndWalkUseCase,
    private val addPooToWalkUseCase: AddPooToWalkUseCase,
    private val addPeeToWalkUseCase: AddPeeToWalkUseCase,
    private val removePooFromWalkUseCase: RemovePooFromWalkUseCase,
    private val removePeeFromWalkUseCase: RemovePeeFromWalkUseCase,
    private val logEventUseCase: LogEventUseCase,
    getActiveWalkUseCase: GetActiveWalkUseCase,
    getWalkHistoryUseCase: GetWalkHistoryUseCase,
    getWalkScheduleUseCase: GetWalkScheduleUseCase,
    private val saveWalkScheduleUseCase: SaveWalkScheduleUseCase
) : ViewModel() {

    val uiState: StateFlow<WalkUiState> = combine(
        getActiveWalkUseCase(),
        getWalkHistoryUseCase()
    ) { active, history -> Pair(active, history) }
        .combine(getWalkScheduleUseCase()) { (active, history), schedule ->
            WalkUiState(activeWalk = active, history = history, schedule = schedule)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalkUiState())

    fun startWalk(context: Context) {
        viewModelScope.launch {
            startWalkUseCase()
            context.startForegroundService(Intent(context, WalkTimerService::class.java))
        }
    }

    fun endWalk(context: Context) {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id ?: return@launch
            endWalkUseCase(walkId)
            context.stopService(Intent(context, WalkTimerService::class.java))
        }
    }

    fun recordPoo() {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id
            if (walkId != null) {
                addPooToWalkUseCase(walkId)
            } else {
                logEventUseCase(type = EventType.POO)
            }
        }
    }

    fun recordPee() {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id
            if (walkId != null) {
                addPeeToWalkUseCase(walkId)
            } else {
                logEventUseCase(type = EventType.PEE)
            }
        }
    }

    fun removePoo(timestampMillis: Long) {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id ?: return@launch
            removePooFromWalkUseCase(walkId, timestampMillis)
        }
    }

    fun removePee(timestampMillis: Long) {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id ?: return@launch
            removePeeFromWalkUseCase(walkId, timestampMillis)
        }
    }

    fun saveSchedule(config: WalkScheduleConfig) {
        viewModelScope.launch { saveWalkScheduleUseCase(config) }
    }

    companion object {
        fun provideFactory(
            startWalkUseCase: StartWalkUseCase,
            endWalkUseCase: EndWalkUseCase,
            addPooToWalkUseCase: AddPooToWalkUseCase,
            addPeeToWalkUseCase: AddPeeToWalkUseCase,
            removePooFromWalkUseCase: RemovePooFromWalkUseCase,
            removePeeFromWalkUseCase: RemovePeeFromWalkUseCase,
            logEventUseCase: LogEventUseCase,
            getActiveWalkUseCase: GetActiveWalkUseCase,
            getWalkHistoryUseCase: GetWalkHistoryUseCase,
            getWalkScheduleUseCase: GetWalkScheduleUseCase,
            saveWalkScheduleUseCase: SaveWalkScheduleUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = WalkViewModel(
                startWalkUseCase, endWalkUseCase, addPooToWalkUseCase, addPeeToWalkUseCase,
                removePooFromWalkUseCase, removePeeFromWalkUseCase, logEventUseCase,
                getActiveWalkUseCase, getWalkHistoryUseCase, getWalkScheduleUseCase, saveWalkScheduleUseCase
            ) as T
        }
    }
}
