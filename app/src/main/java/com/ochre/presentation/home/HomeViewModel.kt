package com.ochre.presentation.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.EventType
import com.ochre.domain.model.WalkSession
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.alone.EndAloneUseCase
import com.ochre.domain.usecase.alone.GetActiveAloneSessionUseCase
import com.ochre.domain.usecase.alone.StartAloneUseCase
import com.ochre.domain.usecase.walk.EndWalkUseCase
import com.ochre.domain.usecase.walk.GetActiveWalkUseCase
import com.ochre.domain.usecase.walk.StartWalkUseCase
import com.ochre.service.AloneTimerService
import com.ochre.service.WalkTimerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeWalk: WalkSession? = null,
    val activeAlone: AloneSession? = null
) {
    val isWalkActive: Boolean get() = activeWalk != null
    val isAloneActive: Boolean get() = activeAlone != null
}

class HomeViewModel(
    private val logEventUseCase: LogEventUseCase,
    private val startWalkUseCase: StartWalkUseCase,
    private val endWalkUseCase: EndWalkUseCase,
    getActiveWalkUseCase: GetActiveWalkUseCase,
    private val startAloneUseCase: StartAloneUseCase,
    private val endAloneUseCase: EndAloneUseCase,
    getActiveAloneSessionUseCase: GetActiveAloneSessionUseCase
) : ViewModel() {

    private val _navigateToWalk = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToWalk: SharedFlow<Unit> = _navigateToWalk.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        getActiveWalkUseCase(),
        getActiveAloneSessionUseCase()
    ) { activeWalk, activeAlone ->
        HomeUiState(activeWalk = activeWalk, activeAlone = activeAlone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun logNote(note: String) {
        viewModelScope.launch {
            logEventUseCase(type = EventType.NOTE, note = note)
        }
    }

    fun startWalk(context: Context) {
        viewModelScope.launch {
            startWalkUseCase()
            context.startForegroundService(Intent(context, WalkTimerService::class.java))
            _navigateToWalk.emit(Unit)
        }
    }

    fun endWalk(context: Context) {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id ?: return@launch
            endWalkUseCase(walkId)
            context.stopService(Intent(context, WalkTimerService::class.java))
        }
    }

    fun startAlone(context: Context) {
        viewModelScope.launch {
            startAloneUseCase()
            context.startForegroundService(Intent(context, AloneTimerService::class.java))
        }
    }

    fun endAlone(context: Context) {
        viewModelScope.launch {
            val sessionId = uiState.value.activeAlone?.id ?: return@launch
            endAloneUseCase(sessionId)
            context.stopService(Intent(context, AloneTimerService::class.java))
        }
    }

    companion object {
        fun provideFactory(
            logEventUseCase: LogEventUseCase,
            startWalkUseCase: StartWalkUseCase,
            endWalkUseCase: EndWalkUseCase,
            getActiveWalkUseCase: GetActiveWalkUseCase,
            startAloneUseCase: StartAloneUseCase,
            endAloneUseCase: EndAloneUseCase,
            getActiveAloneSessionUseCase: GetActiveAloneSessionUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
                logEventUseCase, startWalkUseCase, endWalkUseCase, getActiveWalkUseCase,
                startAloneUseCase, endAloneUseCase, getActiveAloneSessionUseCase
            ) as T
        }
    }
}
