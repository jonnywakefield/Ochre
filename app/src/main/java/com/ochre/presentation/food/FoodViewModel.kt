package com.ochre.presentation.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.MealScheduleEntry
import com.ochre.domain.usecase.DeleteEventUseCase
import com.ochre.domain.usecase.food.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FoodUiState(
    val meals: List<MealScheduleEntry> = emptyList(),
    val feedLog: List<DogEvent> = emptyList(),
    val stockGrams: Int = 0,
    val avgDailyGrams: Float = 0f
) {
    val stockDaysRemaining: Int
        get() = if (avgDailyGrams > 0f) (stockGrams / avgDailyGrams).toInt() else 0
}

class FoodViewModel(
    private val logFeedUseCase: LogFeedUseCase,
    private val getMealScheduleUseCase: GetMealScheduleUseCase,
    private val saveMealUseCase: SaveMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val getFeedLogUseCase: GetFeedLogUseCase,
    private val getCurrentStockUseCase: GetCurrentStockUseCase,
    private val addStockUseCase: AddStockUseCase,
    private val deleteEventUseCase: DeleteEventUseCase
) : ViewModel() {

    val uiState: StateFlow<FoodUiState> = combine(
        getMealScheduleUseCase(),
        getFeedLogUseCase()
    ) { meals, feedLog ->
        Pair(meals, feedLog)
    }.combine(getCurrentStockUseCase()) { (meals, feedLog), stock ->
        val avgDaily = computeAvgDailyGrams(feedLog)
        FoodUiState(meals = meals, feedLog = feedLog, stockGrams = stock, avgDailyGrams = avgDaily)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodUiState())

    fun logFeed(grams: Int, mealId: Long? = null) {
        viewModelScope.launch { logFeedUseCase(grams, mealId) }
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

    private fun computeAvgDailyGrams(feedLog: List<DogEvent>): Float {
        if (feedLog.isEmpty()) return 0f
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recent = feedLog.filter { it.timestampMillis >= cutoff }
        if (recent.isEmpty()) return 0f
        val totalGrams = recent.sumOf { (it.value ?: 0f).toDouble() }.toFloat()
        val daysWithData = recent
            .map { event ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = event.timestampMillis }
                cal.get(java.util.Calendar.YEAR) * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
            }
            .toSet()
            .size
        return totalGrams / daysWithData.coerceAtLeast(1).toFloat()
    }

    companion object {
        fun provideFactory(
            logFeedUseCase: LogFeedUseCase,
            getMealScheduleUseCase: GetMealScheduleUseCase,
            saveMealUseCase: SaveMealUseCase,
            deleteMealUseCase: DeleteMealUseCase,
            getFeedLogUseCase: GetFeedLogUseCase,
            getCurrentStockUseCase: GetCurrentStockUseCase,
            addStockUseCase: AddStockUseCase,
            deleteEventUseCase: DeleteEventUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FoodViewModel(
                logFeedUseCase, getMealScheduleUseCase, saveMealUseCase, deleteMealUseCase,
                getFeedLogUseCase, getCurrentStockUseCase, addStockUseCase, deleteEventUseCase
            ) as T
        }
    }
}
