package com.ochre.presentation.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.model.MealScheduleEntry
import com.ochre.domain.model.WalkSession
import com.ochre.domain.usecase.DeleteEventUseCase
import com.ochre.domain.usecase.LogEventUseCase
import com.ochre.domain.usecase.alone.EndAloneUseCase
import com.ochre.domain.usecase.alone.GetActiveAloneSessionUseCase
import com.ochre.domain.usecase.alone.StartAloneUseCase
import com.ochre.domain.usecase.food.AddStockUseCase
import com.ochre.domain.usecase.food.DeleteMealUseCase
import com.ochre.domain.usecase.food.GetCurrentStockUseCase
import com.ochre.domain.usecase.food.GetFeedLogUseCase
import com.ochre.domain.usecase.food.GetMealScheduleUseCase
import com.ochre.domain.usecase.food.LogFeedUseCase
import com.ochre.domain.usecase.food.SaveMealUseCase
import com.ochre.domain.usecase.walk.EndWalkUseCase
import com.ochre.domain.usecase.walk.GetActiveWalkUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import com.ochre.domain.usecase.walk.StartWalkUseCase
import com.ochre.service.AlarmHelper
import com.ochre.service.AloneTimerService
import com.ochre.service.NotificationPrefs
import com.ochre.service.OchreAlarmScheduler
import com.ochre.service.WalkTimerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    // Walk / alone
    val activeWalk: WalkSession? = null,
    val activeAlone: AloneSession? = null,
    val lastWalkEndMillis: Long? = null,
    // Food
    val lastFedMillis: Long? = null,
    val stockGrams: Int = 0,
    val nextMeal: MealScheduleEntry? = null,
    val nextMealMinutes: Int? = null,   // 0 = window open; >0 = minutes until window opens
    val meals: List<MealScheduleEntry> = emptyList(),
    val feedLog: List<DogEvent> = emptyList(),
    val avgDailyGrams: Float = 0f
) {
    val isWalkActive: Boolean get() = activeWalk != null
    val isAloneActive: Boolean get() = activeAlone != null
    val stockDaysRemaining: Int
        get() = if (avgDailyGrams > 0f) (stockGrams / avgDailyGrams).toInt() else 0
}

class HomeViewModel(
    private val logEventUseCase: LogEventUseCase,
    private val startWalkUseCase: StartWalkUseCase,
    private val endWalkUseCase: EndWalkUseCase,
    getActiveWalkUseCase: GetActiveWalkUseCase,
    getWalkHistoryUseCase: GetWalkHistoryUseCase,
    private val startAloneUseCase: StartAloneUseCase,
    private val endAloneUseCase: EndAloneUseCase,
    getActiveAloneSessionUseCase: GetActiveAloneSessionUseCase,
    private val logFeedUseCase: LogFeedUseCase,
    getFeedLogUseCase: GetFeedLogUseCase,
    getMealScheduleUseCase: GetMealScheduleUseCase,
    getCurrentStockUseCase: GetCurrentStockUseCase,
    private val saveMealUseCase: SaveMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val addStockUseCase: AddStockUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {

    private val _navigateToWalk = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToWalk: SharedFlow<Unit> = _navigateToWalk.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(getActiveWalkUseCase(), getActiveAloneSessionUseCase()) { walk, alone -> Pair(walk, alone) },
        getWalkHistoryUseCase()
    ) { (activeWalk, activeAlone), walks ->
        val lastWalkEnd = walks.filter { !it.isActive }
            .maxByOrNull { it.endMillis ?: it.startMillis }?.endMillis
        Triple(activeWalk, activeAlone, lastWalkEnd)
    }.combine(
        combine(getFeedLogUseCase(), getMealScheduleUseCase()) { feeds, meals -> Pair(feeds, meals) }
    ) { (activeWalk, activeAlone, lastWalkEnd), (feeds, meals) ->
        val lastFed = feeds.maxByOrNull { it.timestampMillis }?.timestampMillis
        val avgDaily = computeAvgDailyGrams(feeds)
        Pair(
            HomeUiState(
                activeWalk = activeWalk,
                activeAlone = activeAlone,
                lastWalkEndMillis = lastWalkEnd,
                lastFedMillis = lastFed,
                meals = meals,
                feedLog = feeds,
                avgDailyGrams = avgDaily
            ),
            meals
        )
    }.combine(getCurrentStockUseCase()) { (partial, meals), stock ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val (nextMeal, nextMealMin) = meals
            .filter { it.randomReminderEnabled }
            .mapNotNull { meal ->
                val centerMin   = meal.targetHour * 60 + meal.targetMinute
                val windowOpen  = (centerMin - meal.windowMinutes / 2 + 1440) % 1440
                val windowClose = (centerMin + meal.windowMinutes / 2) % 1440
                val isOpen = if (windowOpen <= windowClose) {
                    nowMin in windowOpen until windowClose
                } else {
                    nowMin >= windowOpen || nowMin < windowClose
                }
                val minutesUntilOpen = if (isOpen) 0 else (windowOpen - nowMin + 1440) % 1440
                if (isOpen || minutesUntilOpen > 0) Pair(meal, minutesUntilOpen) else null
            }
            .minByOrNull { it.second }
            ?: Pair(null, null)
        partial.copy(stockGrams = stock, nextMeal = nextMeal, nextMealMinutes = nextMealMin)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    // ── Walk ──────────────────────────────────────────────────────────────────

    fun startWalk(context: Context) {
        viewModelScope.launch {
            startWalkUseCase()
            context.startForegroundService(Intent(context, WalkTimerService::class.java))
            // Walk started — cancel gap alert
            OchreAlarmScheduler.cancelWalkGapAlert(context)
            _navigateToWalk.emit(Unit)
        }
    }

    fun endWalk(context: Context) {
        viewModelScope.launch {
            val walkId = uiState.value.activeWalk?.id ?: return@launch
            endWalkUseCase(walkId)
            context.stopService(Intent(context, WalkTimerService::class.java))
            val prefs = NotificationPrefs.get(context)
            val now = System.currentTimeMillis()
            // Reschedule gap alert from now
            if (prefs.walkGapEnabled) {
                OchreAlarmScheduler.scheduleWalkGapAlert(context, now + prefs.walkLimitMinutes * 60_000L)
            }
            // Reschedule any walk scheduled-time alerts for next occurrence
            if (prefs.walkSchedEnabled) {
                AlarmHelper.rescheduleWalkTimeAlerts(context)
            }
        }
    }

    fun startAlone(context: Context) {
        viewModelScope.launch {
            startAloneUseCase()
            context.startForegroundService(Intent(context, AloneTimerService::class.java))
            // Schedule alone alert when limit is reached
            val prefs = NotificationPrefs.get(context)
            val triggerAt = System.currentTimeMillis() + prefs.aloneMaxMinutes * 60_000L
            OchreAlarmScheduler.scheduleAloneAlert(context, triggerAt)
        }
    }

    fun endAlone(context: Context) {
        viewModelScope.launch {
            val sessionId = uiState.value.activeAlone?.id ?: return@launch
            endAloneUseCase(sessionId)
            context.stopService(Intent(context, AloneTimerService::class.java))
            // Dog is home — cancel alone alert
            OchreAlarmScheduler.cancelAloneAlert(context)
        }
    }

    // ── Food ──────────────────────────────────────────────────────────────────

    fun logFeed(grams: Int, mealId: Long? = null, context: Context? = null) {
        viewModelScope.launch {
            logFeedUseCase(grams, mealId)
            // Cancel any pending food alert — dog has been fed
            context?.let { OchreAlarmScheduler.cancelFoodAlert(it) }
        }
    }

    fun saveMeal(entry: MealScheduleEntry) {
        viewModelScope.launch { saveMealUseCase(entry) }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { deleteMealUseCase(id) }
    }

    fun addStock(grams: Int) {
        viewModelScope.launch { addStockUseCase(grams) }
    }

    fun deleteFeedEvent(event: DogEvent) {
        viewModelScope.launch { deleteEventUseCase(event) }
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    fun logNote(note: String) {
        viewModelScope.launch {
            logEventUseCase(type = EventType.NOTE, note = note)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeAvgDailyGrams(feedLog: List<DogEvent>): Float {
        if (feedLog.isEmpty()) return 0f
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recent = feedLog.filter { it.timestampMillis >= cutoff }
        if (recent.isEmpty()) return 0f
        val totalGrams = recent.sumOf { (it.value ?: 0f).toDouble() }.toFloat()
        val daysWithData = recent
            .map { event ->
                val cal = Calendar.getInstance().apply { timeInMillis = event.timestampMillis }
                cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
            }
            .toSet().size
        return totalGrams / daysWithData.coerceAtLeast(1).toFloat()
    }

    companion object {
        fun provideFactory(
            logEventUseCase: LogEventUseCase,
            startWalkUseCase: StartWalkUseCase,
            endWalkUseCase: EndWalkUseCase,
            getActiveWalkUseCase: GetActiveWalkUseCase,
            getWalkHistoryUseCase: GetWalkHistoryUseCase,
            startAloneUseCase: StartAloneUseCase,
            endAloneUseCase: EndAloneUseCase,
            getActiveAloneSessionUseCase: GetActiveAloneSessionUseCase,
            logFeedUseCase: LogFeedUseCase,
            getFeedLogUseCase: GetFeedLogUseCase,
            getMealScheduleUseCase: GetMealScheduleUseCase,
            getCurrentStockUseCase: GetCurrentStockUseCase,
            saveMealUseCase: SaveMealUseCase,
            deleteMealUseCase: DeleteMealUseCase,
            addStockUseCase: AddStockUseCase,
            deleteEventUseCase: DeleteEventUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(
                logEventUseCase, startWalkUseCase, endWalkUseCase, getActiveWalkUseCase,
                getWalkHistoryUseCase, startAloneUseCase, endAloneUseCase,
                getActiveAloneSessionUseCase, logFeedUseCase, getFeedLogUseCase,
                getMealScheduleUseCase, getCurrentStockUseCase, saveMealUseCase,
                deleteMealUseCase, addStockUseCase, deleteEventUseCase
            ) as T
        }
    }
}
