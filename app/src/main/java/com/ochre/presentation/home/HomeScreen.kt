package com.ochre.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.MealScheduleEntry
import com.ochre.presentation.common.OchreColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNoteSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showLogFeed by remember { mutableStateOf(false) }
    var showAddMeal by remember { mutableStateOf(false) }
    var showAddStock by remember { mutableStateOf(false) }
    var feedForMeal by remember { mutableStateOf<MealScheduleEntry?>(null) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item("header") {
            Spacer(Modifier.height(44.dp))
            Text(
                text = "Ochre",
                color = OchreColors.Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Status dashboard ────────────────────────────────────────────────
        item("dashboard") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OchreColors.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, OchreColors.Accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeWalk = uiState.activeWalk
                val lastWalkEnd = uiState.lastWalkEndMillis
                DashRow(
                    label = when {
                        activeWalk != null  -> "Walking  ${formatElapsed(now - activeWalk.startMillis)}"
                        lastWalkEnd != null -> "Last walk  ${formatElapsed(now - lastWalkEnd)} ago"
                        else                -> "Last walk  —"
                    },
                    highlight = uiState.isWalkActive
                )

                HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)

                val nextMeal = uiState.nextMeal
                val nextMealMinutes = uiState.nextMealMinutes
                val feedValue = when {
                    nextMeal != null && nextMealMinutes != null -> buildString {
                        append(nextMeal.label)
                        append("  ")
                        when {
                            nextMealMinutes == 0    -> append("now")
                            nextMealMinutes < 60    -> append("in ${nextMealMinutes}m")
                            else                    -> append("in ${nextMealMinutes / 60}h ${nextMealMinutes % 60}m")
                        }
                    }
                    else -> null
                }
                DashRow(
                    label = if (uiState.lastFedMillis != null)
                        "Last fed  ${formatElapsed(now - uiState.lastFedMillis!!)} ago"
                    else "Last fed  —",
                    value = feedValue
                )

                if (uiState.stockGrams > 0) {
                    HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)
                    DashRow(
                        label = "Stock  ${"%,d".format(uiState.stockGrams)}g",
                        warn = uiState.stockGrams < 500
                    )
                }

                if (uiState.activeAlone != null) {
                    HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)
                    DashRow(
                        label = "Away  ${formatElapsed(now - uiState.activeAlone!!.startMillis)}",
                        highlight = true
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Action buttons ──────────────────────────────────────────────────
        item("actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (uiState.isWalkActive) viewModel.endWalk(context)
                        else viewModel.startWalk(context)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isWalkActive) OchreColors.Accent else Color(0xFF2A2218),
                        contentColor   = if (uiState.isWalkActive) OchreColors.Background else OchreColors.Accent
                    )
                ) {
                    Text(
                        if (uiState.isWalkActive) "End walk" else "Start walk",
                        fontSize = 15.sp, fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        if (uiState.isAloneActive) viewModel.endAlone(context)
                        else viewModel.startAlone(context)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A),
                        contentColor   = OchreColors.TextPrimary
                    )
                ) {
                    Text(
                        if (uiState.isAloneActive) "Back home" else "Leave",
                        fontSize = 15.sp, fontWeight = FontWeight.Normal
                    )
                }

                TextButton(
                    onClick = { showNoteSheet = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Write note", fontSize = 14.sp, color = OchreColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // ── Meals header ────────────────────────────────────────────────────
        item("meals_header") {
            FoodSectionHeader(
                title = "Meals",
                actionLabel = "+ Add meal",
                onAction = { showAddMeal = true }
            )
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.meals.isEmpty()) {
            item("meals_empty") {
                Text("No meals configured", color = OchreColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
        } else {
            items(uiState.meals, key = { "meal_${it.id}" }) { meal ->
                val todayFedForMeal = uiState.feedLog.any { event ->
                    event.isToday() && event.note == "meal:${meal.id}"
                }
                MealRow(
                    meal = meal,
                    fedToday = todayFedForMeal,
                    onFed = { feedForMeal = meal; showLogFeed = true },
                    onDelete = { viewModel.deleteMeal(meal.id) }
                )
            }
        }

        // ── Stock ───────────────────────────────────────────────────────────
        item("stock") {
            Spacer(Modifier.height(24.dp))
            FoodSectionHeader(title = "Stock", actionLabel = "+ Add stock", onAction = { showAddStock = true })
            Spacer(Modifier.height(8.dp))
            StockRow(grams = uiState.stockGrams, daysRemaining = uiState.stockDaysRemaining)
            Spacer(Modifier.height(24.dp))
        }

        // ── Feed log ────────────────────────────────────────────────────────
        item("feedlog_header") {
            FoodSectionHeader(
                title = "Feed log",
                actionLabel = "+ Log feed",
                onAction = { feedForMeal = null; showLogFeed = true }
            )
            Spacer(Modifier.height(4.dp))
        }

        val todayFeeds = uiState.feedLog.filter { it.isToday() }
        val olderFeeds = uiState.feedLog.filter { !it.isToday() }

        if (todayFeeds.isEmpty() && olderFeeds.isEmpty()) {
            item("feedlog_empty") {
                Text("Nothing logged yet", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            if (todayFeeds.isNotEmpty()) {
                item("feedlog_today_label") {
                    Text("Today", color = OchreColors.TextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                }
                items(todayFeeds, key = { "feed_${it.id}" }) { event ->
                    FeedLogRow(event, meals = uiState.meals, onDelete = { viewModel.deleteFeedEvent(event) })
                }
            }
            if (olderFeeds.isNotEmpty()) {
                item("feedlog_older_label") {
                    Spacer(Modifier.height(12.dp))
                    Text("Earlier", color = OchreColors.TextSecondary, fontSize = 10.sp, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(4.dp))
                }
                items(olderFeeds.take(20), key = { "feedold_${it.id}" }) { event ->
                    FeedLogRow(event, meals = uiState.meals, onDelete = { viewModel.deleteFeedEvent(event) })
                }
            }
        }

        item("footer") { Spacer(Modifier.height(32.dp)) }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showLogFeed) {
        LogFeedDialog(
            defaultGrams = feedForMeal?.let {
                if (it.varyAmount) (it.minGrams..it.maxGrams).random() else it.defaultGrams
            } ?: 200,
            mealLabel = feedForMeal?.label,
            onConfirm = { grams ->
                viewModel.logFeed(grams, feedForMeal?.id, context)
                showLogFeed = false
                feedForMeal = null
            },
            onDismiss = { showLogFeed = false; feedForMeal = null }
        )
    }

    if (showAddMeal) {
        AddMealDialog(
            onConfirm = { viewModel.saveMeal(it); showAddMeal = false },
            onDismiss = { showAddMeal = false }
        )
    }

    if (showAddStock) {
        AddStockDialog(
            onConfirm = { viewModel.addStock(it); showAddStock = false },
            onDismiss = { showAddStock = false }
        )
    }

    if (showNoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNoteSheet = false },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            NoteSheet(
                onConfirm = { note -> viewModel.logNote(note); showNoteSheet = false },
                onDismiss = { showNoteSheet = false }
            )
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun DashRow(label: String, value: String? = null, highlight: Boolean = false, warn: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = when { highlight -> OchreColors.Accent; warn -> OchreColors.Destructive; else -> OchreColors.TextPrimary },
            fontSize = 14.sp
        )
        if (value != null) {
            Text(text = value, color = OchreColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FoodSectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = OchreColors.TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Text(actionLabel, color = OchreColors.Accent, fontSize = 13.sp,
            modifier = Modifier.clickable { onAction() })
    }
}

@Composable
private fun MealRow(meal: MealScheduleEntry, fedToday: Boolean, onFed: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(meal.label, color = OchreColors.TextPrimary, fontSize = 15.sp)
                if (fedToday) Text("✓", color = OchreColors.Accent, fontSize = 13.sp)
            }
            val windowStartMin = (meal.targetHour * 60 + meal.targetMinute - meal.windowMinutes / 2 + 1440) % 1440
            val windowEndMin   = (meal.targetHour * 60 + meal.targetMinute + meal.windowMinutes / 2) % 1440
            Text(
                "%02d:%02d – %02d:%02d  %dg".format(
                    windowStartMin / 60, windowStartMin % 60,
                    windowEndMin / 60, windowEndMin % 60,
                    meal.defaultGrams
                ),
                color = OchreColors.TextSecondary, fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onFed,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (fedToday) OchreColors.Surface else OchreColors.Accent,
                    contentColor   = if (fedToday) OchreColors.Accent else OchreColors.Background
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Log", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete meal",
                    tint = OchreColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StockRow(grams: Int, daysRemaining: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${"%,d".format(grams)}g in stock", color = OchreColors.TextPrimary, fontSize = 15.sp)
        Text(
            "~$daysRemaining days",
            color = if (daysRemaining in 1..3) OchreColors.Destructive else OchreColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FeedLogRow(event: DogEvent, meals: List<MealScheduleEntry>, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val mealId = event.note?.removePrefix("meal:")?.toLongOrNull()
    val mealLabel = meals.firstOrNull { it.id == mealId }?.label
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                if (event.isToday()) timeFmt.format(Date(event.timestampMillis))
                else dateFmt.format(Date(event.timestampMillis)),
                color = OchreColors.TextSecondary, fontSize = 13.sp
            )
            if (mealLabel != null) Text(mealLabel, color = OchreColors.TextSecondary, fontSize = 11.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${event.value?.toInt() ?: 0}g", color = OchreColors.TextPrimary, fontSize = 14.sp)
            Icon(Icons.Default.Delete, contentDescription = "Delete",
                tint = OchreColors.TextSecondary,
                modifier = Modifier.size(16.dp).clickable { onDelete() })
        }
    }
}

@Composable
private fun LogFeedDialog(defaultGrams: Int, mealLabel: String?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var gramsText by remember { mutableStateOf(defaultGrams.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text(mealLabel ?: "Log feed", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it },
                label = { Text("Grams", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = dialogFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val g = gramsText.toIntOrNull()
                if (g != null && g in 1..2000) onConfirm(g)
            }) { Text("Log", color = OchreColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) } }
    )
}

@Composable
private fun AddMealDialog(onConfirm: (MealScheduleEntry) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var window by remember { mutableStateOf("60") }
    var grams by remember { mutableStateOf("200") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Add meal", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it; error = null },
                    label = { Text("Label", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. Morning", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true, colors = dialogFieldColors())
                OutlinedTextField(value = time, onValueChange = { time = it; error = null },
                    label = { Text("Time (HH:MM)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true, colors = dialogFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = window, onValueChange = { window = it; error = null },
                        label = { Text("Window (min)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = dialogFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = grams, onValueChange = { grams = it; error = null },
                        label = { Text("Default (g)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = dialogFieldColors(), modifier = Modifier.weight(1f))
                }
                if (error != null) Text(error!!, color = OchreColors.Destructive, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = time.trim().split(":").mapNotNull { it.toIntOrNull() }
                val h = parts.getOrNull(0); val m = parts.getOrNull(1)
                val w = window.toIntOrNull(); val g = grams.toIntOrNull()
                when {
                    label.isBlank()                          -> error = "Label is required"
                    parts.size != 2 || h == null || m == null -> error = "Time must be HH:MM"
                    h !in 0..23                              -> error = "Hour must be 0–23"
                    m !in 0..59                              -> error = "Minute must be 0–59"
                    w == null || w !in 1..180                -> error = "Window must be 1–180 min"
                    g == null || g !in 1..2000               -> error = "Grams must be 1–2000"
                    else -> onConfirm(MealScheduleEntry(label = label, targetHour = h, targetMinute = m, windowMinutes = w, defaultGrams = g))
                }
            }) { Text("Add", color = OchreColors.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) } }
    )
}

@Composable
private fun AddStockDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var gramsText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Add stock", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            OutlinedTextField(
                value = gramsText, onValueChange = { gramsText = it },
                label = { Text("Grams added", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = dialogFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { gramsText.toIntOrNull()?.let { if (it > 0) onConfirm(it) } }) {
                Text("Add", color = OchreColors.Accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) } }
    )
}

@Composable
private fun NoteSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Note", color = OchreColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Light)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            placeholder = { Text("Write a note...", color = OchreColors.TextSecondary, fontSize = 14.sp) },
            colors = dialogFieldColors()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary, fontSize = 14.sp) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor     = OchreColors.TextPrimary,
    unfocusedTextColor   = OchreColors.TextPrimary,
    cursorColor          = OchreColors.Accent
)

private fun formatElapsed(millis: Long): String {
    val totalMin = millis.coerceAtLeast(0L) / 60_000
    val h = totalMin / 60; val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun DogEvent.isToday(): Boolean {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR); val todayYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestampMillis
    return cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
}
