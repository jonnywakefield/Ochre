package com.ochre.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.Reminder
import com.ochre.domain.model.WalkSession
import java.util.concurrent.TimeUnit
import com.ochre.presentation.common.EventSheet
import com.ochre.presentation.common.OchreColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingEvent by remember { mutableStateOf<DogEvent?>(null) }
    var showAddEntry by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var expandedEventId by remember { mutableLongStateOf(-1L) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Calendar",
                color = OchreColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("+ Entry", color = OchreColors.Accent, fontSize = 13.sp,
                    modifier = Modifier.clickable { showAddEntry = true })
                Text("+ Reminder", color = OchreColors.Accent, fontSize = 13.sp,
                    modifier = Modifier.clickable { showAddReminder = true })
            }
        }

        // View toggle
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(CalendarView.entries) { view ->
                val selected = uiState.view == view
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) OchreColors.Accent else OchreColors.Surface,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.setView(view) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        view.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (selected) OchreColors.Background else OchreColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        when (uiState.view) {
            CalendarView.MONTH -> MonthView(
                uiState = uiState,
                onDaySelect = { viewModel.selectDay(it); viewModel.setView(CalendarView.DAY) }
            )
            CalendarView.WEEK -> WeekView(
                uiState = uiState,
                onDaySelect = { viewModel.selectDay(it); viewModel.setView(CalendarView.DAY) }
            )
            CalendarView.DAY -> DayView(
                uiState = uiState,
                expandedEventId = expandedEventId,
                onToggleEvent = { id -> expandedEventId = if (expandedEventId == id) -1L else id },
                onEditEvent = { editingEvent = it },
                onDeleteEvent = { viewModel.deleteEvent(it) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                onDeleteWalk = { viewModel.deleteWalk(it) },
                onDeleteAlone = { viewModel.deleteAlone(it) }
            )
            CalendarView.TIMELINE -> TimelineView(
                uiState = uiState,
                expandedEventId = expandedEventId,
                onToggleEvent = { id -> expandedEventId = if (expandedEventId == id) -1L else id },
                onEditEvent = { editingEvent = it },
                onDeleteEvent = { viewModel.deleteEvent(it) },
                onDeleteWalk = { viewModel.deleteWalk(it) },
                onDeleteAlone = { viewModel.deleteAlone(it) }
            )
        }
    }

    // Edit event sheet
    if (editingEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { editingEvent = null },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            EventSheet(
                existingEvent = editingEvent,
                onConfirm = { _, ts, value, note ->
                    viewModel.updateEvent(editingEvent!!, ts, value, note)
                    editingEvent = null
                },
                onDismiss = { editingEvent = null }
            )
        }
    }

    // Add past entry sheet
    if (showAddEntry) {
        ModalBottomSheet(
            onDismissRequest = { showAddEntry = false },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            EventSheet(
                onConfirm = { type, ts, value, note ->
                    viewModel.logPastEvent(type, ts, value, note)
                    showAddEntry = false
                },
                onDismiss = { showAddEntry = false }
            )
        }
    }

    // Add reminder dialog
    if (showAddReminder) {
        AddReminderDialog(
            onConfirm = { viewModel.saveReminder(it); showAddReminder = false },
            onDismiss = { showAddReminder = false }
        )
    }
}

@Composable
private fun MonthView(uiState: CalendarUiState, onDaySelect: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDayMillis }
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // Navigate months
    var displayMonth by remember { mutableLongStateOf(uiState.selectedDayMillis) }
    val displayCal = Calendar.getInstance().apply { timeInMillis = displayMonth }
    displayCal.set(Calendar.DAY_OF_MONTH, 1)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("<", color = OchreColors.Accent, fontSize = 18.sp,
                modifier = Modifier.clickable {
                    val c = Calendar.getInstance().apply { timeInMillis = displayMonth; add(Calendar.MONTH, -1) }
                    displayMonth = c.timeInMillis
                })
            Text(
                monthFmt.format(Date(displayMonth)),
                color = OchreColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
            Text(">", color = OchreColors.Accent, fontSize = 18.sp,
                modifier = Modifier.clickable {
                    val c = Calendar.getInstance().apply { timeInMillis = displayMonth; add(Calendar.MONTH, 1) }
                    displayMonth = c.timeInMillis
                })
        }
        Spacer(Modifier.height(12.dp))

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(d, color = OchreColors.TextSecondary, fontSize = 11.sp,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        // Build calendar grid
        val firstDay = displayCal.clone() as Calendar
        firstDay.set(Calendar.DAY_OF_MONTH, 1)
        val startOffset = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = startOffset + daysInMonth
        val rows = (cells + 6) / 7

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..daysInMonth) {
                                val dayCal = displayCal.clone() as Calendar
                                dayCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0)
                                dayCal.set(Calendar.SECOND, 0); dayCal.set(Calendar.MILLISECOND, 0)
                                val dayStart = dayCal.timeInMillis
                                val hasEvents = uiState.eventsForDay(dayStart).isNotEmpty() ||
                                               uiState.walksForDay(dayStart).isNotEmpty() ||
                                               uiState.aloneSessionsForDay(dayStart).isNotEmpty() ||
                                               uiState.remindersForDay(dayStart).isNotEmpty()
                                val isToday = dayStart == uiState.selectedDayMillis

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onDaySelect(dayStart) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isToday) OchreColors.Accent else OchreColors.Background),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dayNum.toString(),
                                            color = if (isToday) OchreColors.Background else OchreColors.TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (hasEvents) {
                                        Box(Modifier.size(4.dp).clip(CircleShape).background(OchreColors.Accent))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(uiState: CalendarUiState, onDaySelect: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDayMillis }
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val weekStart = cal.timeInMillis

    val dayFmt = SimpleDateFormat("EEE d", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..6) {
            val dayMillis = weekStart + i * 24 * 60 * 60 * 1000L
            val isSelected = dayMillis == uiState.selectedDayMillis
            val hasEvents = uiState.eventsForDay(dayMillis).isNotEmpty() ||
                uiState.walksForDay(dayMillis).isNotEmpty() ||
                uiState.aloneSessionsForDay(dayMillis).isNotEmpty()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) OchreColors.Accent else OchreColors.Surface)
                    .clickable { onDaySelect(dayMillis) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    dayFmt.format(Date(dayMillis)),
                    color = if (isSelected) OchreColors.Background else OchreColors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                if (hasEvents) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.size(4.dp).clip(CircleShape)
                        .background(if (isSelected) OchreColors.Background else OchreColors.Accent))
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    // Show selected day events below
    DayEventList(
        events = uiState.eventsForDay(uiState.selectedDayMillis),
        walks = uiState.walksForDay(uiState.selectedDayMillis),
        alones = uiState.aloneSessionsForDay(uiState.selectedDayMillis),
        reminders = uiState.remindersForDay(uiState.selectedDayMillis),
        expandedEventId = -1L,
        onToggleEvent = {},
        onEditEvent = {},
        onDeleteEvent = {},
        onDeleteReminder = {},
        onDeleteWalk = {},
        onDeleteAlone = {}
    )
}

@Composable
private fun DayView(
    uiState: CalendarUiState,
    expandedEventId: Long,
    onToggleEvent: (Long) -> Unit,
    onEditEvent: (DogEvent) -> Unit,
    onDeleteEvent: (DogEvent) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onDeleteWalk: (Long) -> Unit,
    onDeleteAlone: (Long) -> Unit
) {
    val dateFmt = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            dateFmt.format(Date(uiState.selectedDayMillis)),
            color = OchreColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        DayEventList(
            events = uiState.eventsForDay(uiState.selectedDayMillis),
            walks = uiState.walksForDay(uiState.selectedDayMillis),
            alones = uiState.aloneSessionsForDay(uiState.selectedDayMillis),
            reminders = uiState.remindersForDay(uiState.selectedDayMillis),
            expandedEventId = expandedEventId,
            onToggleEvent = onToggleEvent,
            onEditEvent = onEditEvent,
            onDeleteEvent = onDeleteEvent,
            onDeleteReminder = onDeleteReminder,
            onDeleteWalk = onDeleteWalk,
            onDeleteAlone = onDeleteAlone
        )
    }
}

@Composable
private fun DayEventList(
    events: List<DogEvent>,
    walks: List<WalkSession>,
    alones: List<AloneSession>,
    reminders: List<Reminder>,
    expandedEventId: Long,
    onToggleEvent: (Long) -> Unit,
    onEditEvent: (DogEvent) -> Unit,
    onDeleteEvent: (DogEvent) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onDeleteWalk: (Long) -> Unit,
    onDeleteAlone: (Long) -> Unit
) {
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    if (events.isEmpty() && walks.isEmpty() && alones.isEmpty() && reminders.isEmpty()) {
        Text("Nothing on this day", color = OchreColors.TextSecondary, fontSize = 13.sp)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items(reminders, key = { "r_${it.id}" }) { reminder ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(timeFmt.format(Date(reminder.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                    Text(reminder.title, color = OchreColors.Accent, fontSize = 14.sp)
                }
                TextButton(onClick = { onDeleteReminder(reminder.id) }, contentPadding = PaddingValues(0.dp)) {
                    Text("delete", color = OchreColors.Destructive, fontSize = 12.sp)
                }
            }
        }
        items(walks, key = { "w_${it.id}" }) { walk ->
            val detail = buildList {
                walk.durationMillis?.let { d ->
                    val h = TimeUnit.MILLISECONDS.toHours(d)
                    val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
                    add(if (h > 0) "${h}h ${m}m" else "${m}m")
                }
                if (walk.pooEvents.size > 0) add("${walk.pooEvents.size} poo")
                if (walk.peeEvents.size > 0) add("${walk.peeEvents.size} pee")
            }.joinToString("  ·  ")
            Column(modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(-walk.id) }.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(timeFmt.format(Date(walk.endMillis ?: walk.startMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                        Text("walk", color = OchreColors.TextPrimary, fontSize = 14.sp)
                    }
                    if (detail.isNotEmpty()) Text(detail, color = OchreColors.TextSecondary, fontSize = 12.sp)
                }
                if (expandedEventId == -walk.id) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        TextButton(onClick = { onDeleteWalk(walk.id) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        items(alones, key = { "a_${it.id}" }) { alone ->
            val detail = alone.durationMillis?.let { d ->
                val h = TimeUnit.MILLISECONDS.toHours(d)
                val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
                if (h > 0) "${h}h ${m}m" else "${m}m"
            } ?: ""
            Column(modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(-alone.id - 1_000_000L) }.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(timeFmt.format(Date(alone.endMillis ?: alone.startMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                        Text("away", color = OchreColors.TextPrimary, fontSize = 14.sp)
                    }
                    if (detail.isNotEmpty()) Text(detail, color = OchreColors.TextSecondary, fontSize = 12.sp)
                }
                if (expandedEventId == -alone.id - 1_000_000L) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        TextButton(onClick = { onDeleteAlone(alone.id) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        items(events, key = { it.id }) { event ->
            Column(
                modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(event.id) }.padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(timeFmt.format(Date(event.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                        Text(event.type.name.lowercase().replaceFirstChar { it.uppercase() }, color = OchreColors.TextPrimary, fontSize = 14.sp)
                    }
                    val preview = when {
                        event.value != null -> when (event.type) {
                            com.ochre.domain.model.EventType.WEIGHT -> "${event.value}kg"
                            com.ochre.domain.model.EventType.FEED -> "${event.value.toInt()}g"
                            else -> event.value.toString()
                        }
                        event.note != null -> event.note.take(20)
                        else -> ""
                    }
                    if (preview.isNotEmpty()) Text(preview, color = OchreColors.TextSecondary, fontSize = 12.sp)
                }
                if (expandedEventId == event.id) {
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        TextButton(onClick = { onEditEvent(event) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Edit", color = OchreColors.Accent, fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDeleteEvent(event) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private sealed class TimelineEntry {
    abstract val sortMillis: Long
    data class Ev(val event: DogEvent) : TimelineEntry() { override val sortMillis get() = event.timestampMillis }
    data class Wk(val session: WalkSession) : TimelineEntry() { override val sortMillis get() = session.endMillis ?: session.startMillis }
    data class Al(val session: AloneSession) : TimelineEntry() { override val sortMillis get() = session.endMillis ?: session.startMillis }
}

@Composable
private fun TimelineView(
    uiState: CalendarUiState,
    expandedEventId: Long,
    onToggleEvent: (Long) -> Unit,
    onEditEvent: (DogEvent) -> Unit,
    onDeleteEvent: (DogEvent) -> Unit,
    onDeleteWalk: (Long) -> Unit,
    onDeleteAlone: (Long) -> Unit
) {
    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    val allEntries: List<TimelineEntry> = (
        uiState.events.map { TimelineEntry.Ev(it) } +
        uiState.walks.map { TimelineEntry.Wk(it) } +
        uiState.aloneSessions.map { TimelineEntry.Al(it) }
    ).sortedByDescending { it.sortMillis }

    val grouped = allEntries.groupBy { dateFmt.format(Date(it.sortMillis)) }

    LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp)) {
        grouped.forEach { (dayLabel, entries) ->
            item(key = dayLabel) {
                Text(dayLabel, color = OchreColors.TextSecondary, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            }
            items(entries, key = { when (it) {
                is TimelineEntry.Ev -> "e_${it.event.id}"
                is TimelineEntry.Wk -> "w_${it.session.id}"
                is TimelineEntry.Al -> "a_${it.session.id}"
            }}) { entry ->
                when (entry) {
                    is TimelineEntry.Ev -> {
                        val event = entry.event
                        Column(modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(event.id) }.padding(vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(timeFmt.format(Date(event.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                                    Text(event.type.name.lowercase().replaceFirstChar { it.uppercase() }, color = OchreColors.TextPrimary, fontSize = 14.sp)
                                }
                                if (event.value != null) Text("${event.value}", color = OchreColors.TextSecondary, fontSize = 12.sp)
                            }
                            if (expandedEventId == event.id) {
                                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    TextButton(onClick = { onEditEvent(event) }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Edit", color = OchreColors.Accent, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onDeleteEvent(event) }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    is TimelineEntry.Wk -> {
                        val walk = entry.session
                        val detail = buildList {
                            walk.durationMillis?.let { d ->
                                val h = TimeUnit.MILLISECONDS.toHours(d); val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
                                add(if (h > 0) "${h}h ${m}m" else "${m}m")
                            }
                            if (walk.pooEvents.size > 0) add("${walk.pooEvents.size} poo")
                            if (walk.peeEvents.size > 0) add("${walk.peeEvents.size} pee")
                        }.joinToString("  ·  ")
                        Column(modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(-walk.id) }.padding(vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(timeFmt.format(Date(walk.endMillis ?: walk.startMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                                    Text("walk", color = OchreColors.TextPrimary, fontSize = 14.sp)
                                }
                                if (detail.isNotEmpty()) Text(detail, color = OchreColors.TextSecondary, fontSize = 12.sp)
                            }
                            if (expandedEventId == -walk.id) {
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    TextButton(onClick = { onDeleteWalk(walk.id) }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    is TimelineEntry.Al -> {
                        val alone = entry.session
                        val detail = alone.durationMillis?.let { d ->
                            val h = TimeUnit.MILLISECONDS.toHours(d); val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
                            if (h > 0) "${h}h ${m}m" else "${m}m"
                        } ?: ""
                        Column(modifier = Modifier.fillMaxWidth().clickable { onToggleEvent(-alone.id - 1_000_000L) }.padding(vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(timeFmt.format(Date(alone.endMillis ?: alone.startMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
                                    Text("away", color = OchreColors.TextPrimary, fontSize = 14.sp)
                                }
                                if (detail.isNotEmpty()) Text(detail, color = OchreColors.TextSecondary, fontSize = 12.sp)
                            }
                            if (expandedEventId == -alone.id - 1_000_000L) {
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    TextButton(onClick = { onDeleteAlone(alone.id) }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Delete", color = OchreColors.Destructive, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddReminderDialog(onConfirm: (Reminder) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Add reminder", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. Vet appointment", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true, colors = reminderFieldColors()
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text("Date (d/M/yyyy)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true, colors = reminderFieldColors()
                )
                OutlinedTextField(
                    value = time, onValueChange = { time = it },
                    label = { Text("Time (HH:MM)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true, colors = reminderFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val fmt = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
                    val ts = fmt.parse("$date $time")?.time ?: return@TextButton
                    if (title.isNotBlank()) onConfirm(Reminder(title = title, timestampMillis = ts))
                } catch (_: Exception) {}
            }) { Text("Add", color = OchreColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) } }
    )
}

@Composable
private fun reminderFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor = OchreColors.TextPrimary,
    unfocusedTextColor = OchreColors.TextPrimary,
    cursorColor = OchreColors.Accent
)
