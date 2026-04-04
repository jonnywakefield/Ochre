package com.ochre.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.presentation.common.EventSheet
import com.ochre.presentation.common.OchreColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var expandedItemKey by remember { mutableStateOf<String?>(null) }
    var editingEvent by remember { mutableStateOf<DogEvent?>(null) }
    var showLogPastSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val grouped = remember(uiState.filtered) {
        uiState.filtered.groupBy { it.dayLabel() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 52.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "history",
                    color = OchreColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "+ past",
                    color = OchreColors.Accent,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { showLogPastSheet = true }
                )
            }

            // Top-level filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item { FilterChip("all", uiState.activeFilter == HistoryFilter.ALL) { viewModel.setFilter(HistoryFilter.ALL) } }
                item { FilterChip("walks", uiState.activeFilter == HistoryFilter.WALKS) { viewModel.setFilter(HistoryFilter.WALKS) } }
                item { FilterChip("alone", uiState.activeFilter == HistoryFilter.ALONE) { viewModel.setFilter(HistoryFilter.ALONE) } }
                item { FilterChip("events", uiState.activeFilter == HistoryFilter.EVENTS) { viewModel.setFilter(HistoryFilter.EVENTS) } }
            }

            // Event sub-filters (only when events tab selected)
            if (uiState.activeFilter == HistoryFilter.EVENTS) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip("all", uiState.activeEventFilter == null) { viewModel.setEventTypeFilter(null) }
                    }
                    items(EventType.entries) { type ->
                        FilterChip(
                            label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = uiState.activeEventFilter == type
                        ) { viewModel.setEventTypeFilter(type) }
                    }
                }
            }

            if (grouped.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "no entries", color = OchreColors.TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                    grouped.forEach { (dayLabel, items) ->
                        item(key = dayLabel) {
                            Text(
                                text = dayLabel,
                                color = OchreColors.TextSecondary,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                            )
                        }
                        items(items, key = { it.itemKey() }) { item ->
                            when (item) {
                                is HistoryItem.Event -> HistoryEventRow(
                                    event = item.event,
                                    expanded = expandedItemKey == item.itemKey(),
                                    onTap = { expandedItemKey = if (expandedItemKey == item.itemKey()) null else item.itemKey() },
                                    onEdit = { editingEvent = item.event },
                                    onDelete = { viewModel.deleteEvent(item.event) }
                                )
                                is HistoryItem.Walk -> HistorySessionRow(
                                    label = "walk",
                                    startMillis = item.session.startMillis,
                                    endMillis = item.session.endMillis,
                                    detail = item.session.let {
                                        val mins = it.durationMillis?.let { d -> d / 60_000 }
                                        buildList {
                                            mins?.let { add("${it}m") }
                                            if (item.session.pooEvents.size > 0) add("${item.session.pooEvents.size} poo")
                                            if (item.session.peeEvents.size > 0) add("${item.session.peeEvents.size} pee")
                                        }.joinToString("  ·  ")
                                    },
                                    expanded = expandedItemKey == item.itemKey(),
                                    onTap = { expandedItemKey = if (expandedItemKey == item.itemKey()) null else item.itemKey() },
                                    onDelete = { viewModel.deleteWalk(item.session.id) }
                                )
                                is HistoryItem.Alone -> HistorySessionRow(
                                    label = "away",
                                    startMillis = item.session.startMillis,
                                    endMillis = item.session.endMillis,
                                    detail = item.session.durationMillis?.let { d ->
                                        val h = TimeUnit.MILLISECONDS.toHours(d)
                                        val m = TimeUnit.MILLISECONDS.toMinutes(d) % 60
                                        if (h > 0) "${h}h ${m}m" else "${m}m"
                                    } ?: "",
                                    expanded = expandedItemKey == item.itemKey(),
                                    onTap = { expandedItemKey = if (expandedItemKey == item.itemKey()) null else item.itemKey() },
                                    onDelete = { viewModel.deleteAlone(item.session.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { editingEvent = null },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            EventSheet(
                existingEvent = editingEvent,
                onConfirm = { _, timestamp, value, note ->
                    viewModel.updateEvent(editingEvent!!, timestamp, value, note)
                    editingEvent = null
                },
                onDismiss = { editingEvent = null }
            )
        }
    }

    if (showLogPastSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogPastSheet = false },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            EventSheet(
                onConfirm = { type, timestamp, value, note ->
                    viewModel.logPastEvent(type, timestamp, value, note)
                    showLogPastSheet = false
                },
                onDismiss = { showLogPastSheet = false }
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) OchreColors.Accent else OchreColors.Surface,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) OchreColors.Background else OchreColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun HistoryEventRow(
    event: DogEvent,
    expanded: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormatter.format(Date(event.timestampMillis)),
                    color = OchreColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = event.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = OchreColors.TextPrimary,
                    fontSize = 15.sp
                )
            }
            val preview = when {
                event.value != null -> formatValue(event.type, event.value)
                event.note != null -> event.note.take(24) + if (event.note.length > 24) "…" else ""
                else -> ""
            }
            if (preview.isNotEmpty()) {
                Text(text = preview, color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }

        if (expanded) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                    Text("edit", color = OchreColors.Accent, fontSize = 13.sp)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("delete", color = OchreColors.Destructive, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun HistorySessionRow(
    label: String,
    startMillis: Long,
    endMillis: Long?,
    detail: String,
    expanded: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayTime = endMillis ?: startMillis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormatter.format(Date(displayTime)),
                    color = OchreColors.TextSecondary,
                    fontSize = 13.sp
                )
                Text(text = label, color = OchreColors.TextPrimary, fontSize = 15.sp)
            }
            if (detail.isNotEmpty()) {
                Text(text = detail, color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }
        if (expanded) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("delete", color = OchreColors.Destructive, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun formatValue(type: EventType, value: Float): String = when (type) {
    EventType.WEIGHT -> "${value}kg"
    EventType.FEED -> "${value.toInt()}g"
    else -> value.toString()
}

private fun HistoryItem.itemKey(): String = when (this) {
    is HistoryItem.Event  -> "event_${event.id}"
    is HistoryItem.Walk   -> "walk_${session.id}"
    is HistoryItem.Alone  -> "alone_${session.id}"
}

private fun HistoryItem.dayLabel(): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestampMillis
    val itemDay = cal.get(Calendar.DAY_OF_YEAR)
    val itemYear = cal.get(Calendar.YEAR)
    return when {
        itemYear == year && itemDay == today     -> "today"
        itemYear == year && itemDay == today - 1 -> "yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestampMillis))
    }
}
