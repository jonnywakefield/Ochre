package com.ochre.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.model.WalkSession
import com.ochre.domain.usecase.GetAllEventsUseCase
import com.ochre.domain.usecase.walk.GetWalkHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class StatsPeriod(val label: String, val days: Int?) {
    DAYS_7("7d", 7),
    DAYS_30("30d", 30),
    DAYS_90("90d", 90),
    ALL("All", null)
}

data class DailyToiletEntry(
    val label: String,
    val dayMillis: Long,
    val peeCount: Int,
    val pooCount: Int
)

data class DailyWalkEntry(
    val label: String,
    val dayMillis: Long,
    val walkCount: Int,
    val avgDurationMinutes: Float
)

// Hour 0..23 → count for each tracked type
data class HourlyBucket(val walkStarts: IntArray = IntArray(24), val pees: IntArray = IntArray(24), val poos: IntArray = IntArray(24), val feeds: IntArray = IntArray(24))

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.DAYS_30,
    val isLoading: Boolean = true,

    val dailyToilet: List<DailyToiletEntry> = emptyList(),
    val dailyWalks: List<DailyWalkEntry> = emptyList(),
    val pooAfterFoodMinutes: List<Int> = emptyList(),   // one value per poo event
    val hourly: HourlyBucket = HourlyBucket(),

    val avgPeesPerDay: Float = 0f,
    val avgPoosPerDay: Float = 0f,
    val avgWalksPerDay: Float = 0f,
    val avgWalkDurationMinutes: Float = 0f,
    val avgPooAfterFoodMinutes: Float? = null,
    val totalPees: Int = 0,
    val totalPoos: Int = 0,
    val totalWalks: Int = 0,
    val daysInPeriod: Int = 0
)

class StatsViewModel(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getWalkHistoryUseCase: GetWalkHistoryUseCase
) : ViewModel() {

    private val _period = MutableStateFlow(StatsPeriod.DAYS_30)
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                getAllEventsUseCase(),
                getWalkHistoryUseCase(),
                _period
            ) { events, walks, period -> Triple(events, walks, period) }
                .collect { (events, walks, period) ->
                    _uiState.value = compute(events, walks, period)
                }
        }
    }

    fun setPeriod(period: StatsPeriod) {
        _period.value = period
    }

    private fun compute(
        allEvents: List<DogEvent>,
        allWalks: List<WalkSession>,
        period: StatsPeriod
    ): StatsUiState {
        val nowMillis = System.currentTimeMillis()
        val cutoffMillis = period.days?.let { nowMillis - TimeUnit.DAYS.toMillis(it.toLong()) } ?: 0L

        val events = allEvents.filter { it.timestampMillis >= cutoffMillis }
        val walks = allWalks.filter { it.startMillis >= cutoffMillis && !it.isActive }
        val feedEvents = events.filter { it.type == EventType.FEED }.sortedBy { it.timestampMillis }

        // Collect all pee/poo timestamps (from walk sessions in period)
        val allPeeTs = mutableListOf<Long>()
        val allPooTs = mutableListOf<Long>()
        allWalks.filter { it.startMillis >= cutoffMillis }.forEach { w ->
            allPeeTs.addAll(w.peeEvents)
            allPooTs.addAll(w.pooEvents)
        }

        // --- Daily buckets ---
        val dayFmt = SimpleDateFormat(if (period == StatsPeriod.DAYS_7) "EEE" else "d MMM", Locale.getDefault())

        val dailyPee = mutableMapOf<Long, Int>()
        val dailyPoo = mutableMapOf<Long, Int>()
        allPeeTs.forEach { ts -> dayOf(ts)?.let { d -> dailyPee[d] = (dailyPee[d] ?: 0) + 1 } }
        allPooTs.forEach { ts -> dayOf(ts)?.let { d -> dailyPoo[d] = (dailyPoo[d] ?: 0) + 1 } }

        val dailyWalkCount = mutableMapOf<Long, Int>()
        val dailyWalkDurSum = mutableMapOf<Long, Long>()
        val dailyWalkDurCnt = mutableMapOf<Long, Int>()
        walks.forEach { w ->
            val d = dayOf(w.startMillis) ?: return@forEach
            dailyWalkCount[d] = (dailyWalkCount[d] ?: 0) + 1
            w.durationMillis?.let { dur ->
                dailyWalkDurSum[d] = (dailyWalkDurSum[d] ?: 0L) + dur
                dailyWalkDurCnt[d] = (dailyWalkDurCnt[d] ?: 0) + 1
            }
        }

        val dayBuckets = buildDayBuckets(cutoffMillis, nowMillis, period, allEvents, allWalks)

        val dailyToilet = dayBuckets.map { d ->
            DailyToiletEntry(
                label = dayFmt.format(java.util.Date(d)),
                dayMillis = d,
                peeCount = dailyPee[d] ?: 0,
                pooCount = dailyPoo[d] ?: 0
            )
        }

        val dailyWalks = dayBuckets.map { d ->
            val cnt = dailyWalkCount[d] ?: 0
            val avgDur = if ((dailyWalkDurCnt[d] ?: 0) > 0)
                (dailyWalkDurSum[d] ?: 0L).toFloat() / (dailyWalkDurCnt[d]!!) / 60_000f
            else 0f
            DailyWalkEntry(
                label = dayFmt.format(java.util.Date(d)),
                dayMillis = d,
                walkCount = cnt,
                avgDurationMinutes = avgDur
            )
        }

        // --- Poo after food ---
        val pooAfterFood = allPooTs.sorted().mapNotNull { pooTs ->
            val lastFeed = feedEvents.lastOrNull { it.timestampMillis <= pooTs }
            lastFeed?.let {
                val diffMins = TimeUnit.MILLISECONDS.toMinutes(pooTs - it.timestampMillis).toInt()
                if (diffMins in 0..600) diffMins else null // ignore implausible gaps > 10h
            }
        }

        // --- 24h distribution ---
        val hourly = HourlyBucket()
        walks.forEach { w -> hourOf(w.startMillis)?.let { h -> hourly.walkStarts[h]++ } }
        allPeeTs.forEach { ts -> hourOf(ts)?.let { h -> hourly.pees[h]++ } }
        allPooTs.forEach { ts -> hourOf(ts)?.let { h -> hourly.poos[h]++ } }
        feedEvents.forEach { e -> hourOf(e.timestampMillis)?.let { h -> hourly.feeds[h]++ } }

        // --- Summaries ---
        val days = dayBuckets.size.coerceAtLeast(1)
        val totalPees = allPeeTs.size
        val totalPoos = allPooTs.size
        val totalWalks = walks.size

        val allDurations = walks.mapNotNull { it.durationMillis }
        val avgDur = if (allDurations.isNotEmpty()) allDurations.average().toFloat() / 60_000f else 0f

        return StatsUiState(
            period = period,
            isLoading = false,
            dailyToilet = dailyToilet,
            dailyWalks = dailyWalks,
            pooAfterFoodMinutes = pooAfterFood,
            hourly = hourly,
            avgPeesPerDay = if (days > 0) totalPees.toFloat() / days else 0f,
            avgPoosPerDay = if (days > 0) totalPoos.toFloat() / days else 0f,
            avgWalksPerDay = if (days > 0) totalWalks.toFloat() / days else 0f,
            avgWalkDurationMinutes = avgDur,
            avgPooAfterFoodMinutes = if (pooAfterFood.isNotEmpty()) pooAfterFood.average().toFloat() else null,
            totalPees = totalPees,
            totalPoos = totalPoos,
            totalWalks = totalWalks,
            daysInPeriod = days
        )
    }

    private fun buildDayBuckets(
        cutoffMillis: Long,
        nowMillis: Long,
        period: StatsPeriod,
        allEvents: List<DogEvent>,
        allWalks: List<WalkSession>
    ): List<Long> {
        val days = mutableListOf<Long>()
        val startMillis = if (period.days != null) {
            cutoffMillis
        } else {
            // For "All", find the actual earliest entry
            val earliestEvent = allEvents.minOfOrNull { it.timestampMillis }
            val earliestWalk = allWalks.minOfOrNull { it.startMillis }
            listOfNotNull(earliestEvent, earliestWalk).minOrNull()
                ?: (nowMillis - TimeUnit.DAYS.toMillis(90))
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        while (cal.timeInMillis <= endOfToday) {
            days.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    private fun dayOf(millis: Long): Long? {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun hourOf(millis: Long): Int? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    companion object {
        fun provideFactory(
            getAllEventsUseCase: GetAllEventsUseCase,
            getWalkHistoryUseCase: GetWalkHistoryUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatsViewModel(getAllEventsUseCase, getWalkHistoryUseCase) as T
        }
    }
}
