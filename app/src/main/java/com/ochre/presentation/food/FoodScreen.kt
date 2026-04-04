package com.ochre.presentation.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun FoodScreen(viewModel: FoodViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogFeed by remember { mutableStateOf(false) }
    var showAddMeal by remember { mutableStateOf(false) }
    var showAddStock by remember { mutableStateOf(false) }
    var feedForMeal by remember { mutableStateOf<MealScheduleEntry?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        item("header") {
            Spacer(Modifier.height(52.dp))
            Text(
                "Food",
                color = OchreColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(28.dp))
        }

        // Meals section
        item("meals_header") {
            SectionHeader(title = "Meals", actionLabel = "+ Add meal", onAction = { showAddMeal = true })
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.meals.isEmpty()) {
            item("meals_empty") {
                Text("No meals configured", color = OchreColors.TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
        } else {
            items(uiState.meals, key = { "meal_${it.id}" }) { meal ->
                MealRow(
                    meal = meal,
                    onFed = {
                        feedForMeal = meal
                        showLogFeed = true
                    },
                    onDelete = { viewModel.deleteMeal(meal.id) }
                )
            }
        }

        // Stock section
        item("stock") {
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Stock", actionLabel = "+ Add stock", onAction = { showAddStock = true })
            Spacer(Modifier.height(8.dp))
            StockRow(
                grams = uiState.stockGrams,
                daysRemaining = uiState.stockDaysRemaining
            )
            Spacer(Modifier.height(24.dp))
        }

        // Feed log section
        item("feedlog_header") {
            SectionHeader(
                title = "Today's log",
                actionLabel = "+ Log feed",
                onAction = {
                    feedForMeal = null
                    showLogFeed = true
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        val todayFeeds = uiState.feedLog.filter { it.isToday() }
        if (todayFeeds.isEmpty()) {
            item("feedlog_empty") { Text("Nothing logged today", color = OchreColors.TextSecondary, fontSize = 13.sp) }
        } else {
            items(todayFeeds, key = { "feed_${it.id}" }) { event ->
                FeedLogRow(event, onDelete = { viewModel.deleteFeedEvent(event) })
            }
        }

        item("footer") { Spacer(Modifier.height(32.dp)) }
    }

    // Log feed sheet
    if (showLogFeed) {
        LogFeedDialog(
            defaultGrams = feedForMeal?.let {
                if (it.varyAmount) (it.minGrams..it.maxGrams).random()
                else it.defaultGrams
            } ?: 200,
            mealLabel = feedForMeal?.label,
            onConfirm = { grams ->
                viewModel.logFeed(grams, feedForMeal?.id)
                showLogFeed = false
                feedForMeal = null
            },
            onDismiss = {
                showLogFeed = false
                feedForMeal = null
            }
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
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
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
private fun MealRow(meal: MealScheduleEntry, onFed: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(meal.label, color = OchreColors.TextPrimary, fontSize = 15.sp)
            val window = "%02d:%02d – %02d:%02d".format(
                meal.targetHour,
                meal.targetMinute,
                (meal.targetHour + (meal.targetMinute + meal.windowMinutes) / 60) % 24,
                (meal.targetMinute + meal.windowMinutes) % 60
            )
            Text(
                "$window  ${meal.defaultGrams}g",
                color = OchreColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onFed,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OchreColors.Accent,
                    contentColor = OchreColors.Background
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Fed", fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
        Text(
            "${"%,d".format(grams)}g in stock",
            color = OchreColors.TextPrimary,
            fontSize = 15.sp
        )
        Text(
            "~$daysRemaining days",
            color = if (daysRemaining < 4) OchreColors.Destructive else OchreColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FeedLogRow(event: DogEvent, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fmt.format(Date(event.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${event.value?.toInt() ?: 0}g", color = OchreColors.TextPrimary, fontSize = 14.sp)
            Icon(Icons.Default.Delete, contentDescription = "Delete feed",
                tint = OchreColors.TextSecondary, modifier = Modifier.size(16.dp).clickable { onDelete() })
        }
    }
}

@Composable
private fun LogFeedDialog(
    defaultGrams: Int,
    mealLabel: String?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
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
                colors = feedFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { gramsText.toIntOrNull()?.let { onConfirm(it) } }) {
                Text("Log", color = OchreColors.Accent)
            }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Add meal", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("Label", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. Morning", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true, colors = feedFieldColors())
                OutlinedTextField(value = time, onValueChange = { time = it },
                    label = { Text("Time (HH:MM)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true, colors = feedFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = window, onValueChange = { window = it },
                        label = { Text("Window (min)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = feedFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = grams, onValueChange = { grams = it },
                        label = { Text("Default (g)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = feedFieldColors(), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = time.split(":").mapNotNull { it.toIntOrNull() }
                if (label.isNotBlank() && parts.size == 2) {
                    onConfirm(MealScheduleEntry(
                        label = label,
                        targetHour = parts[0], targetMinute = parts[1],
                        windowMinutes = window.toIntOrNull() ?: 60,
                        defaultGrams = grams.toIntOrNull() ?: 200
                    ))
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
                colors = feedFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { gramsText.toIntOrNull()?.let { onConfirm(it) } }) {
                Text("Add", color = OchreColors.Accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) } }
    )
}

@Composable
private fun feedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor = OchreColors.TextPrimary,
    unfocusedTextColor = OchreColors.TextPrimary,
    cursorColor = OchreColors.Accent
)

private fun DogEvent.isToday(): Boolean {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestampMillis
    return cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
}
