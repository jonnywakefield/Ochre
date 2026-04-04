package com.ochre.presentation.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.ochre.domain.model.WalkScheduleConfig
import com.ochre.domain.model.WalkScheduleEntry
import com.ochre.presentation.common.OchreColors

@Composable
fun WalkSettingsScreen(
    viewModel: WalkViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.schedule

    var maxGapText by remember(config) { mutableStateOf((config.maxGapMinutes / 60).toString()) }
    var quietFrom by remember(config) { mutableStateOf("%02d:%02d".format(config.quietFromHour, config.quietFromMinute)) }
    var quietTo by remember(config) { mutableStateOf("%02d:%02d".format(config.quietToHour, config.quietToMinute)) }
    var entries by remember(config) { mutableStateOf(config.entries) }
    var showAddEntry by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OchreColors.TextSecondary)
            }
            Text(
                text = "Walk Schedule",
                color = OchreColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            TextButton(onClick = {
                val maxGapHours = maxGapText.toIntOrNull() ?: 6
                val fromParts = quietFrom.split(":").mapNotNull { it.toIntOrNull() }
                val toParts = quietTo.split(":").mapNotNull { it.toIntOrNull() }
                viewModel.saveSchedule(
                    WalkScheduleConfig(
                        entries = entries,
                        maxGapMinutes = maxGapHours * 60,
                        quietFromHour = fromParts.getOrElse(0) { 22 },
                        quietFromMinute = fromParts.getOrElse(1) { 0 },
                        quietToHour = toParts.getOrElse(0) { 7 },
                        quietToMinute = toParts.getOrElse(1) { 0 }
                    )
                )
                onBack()
            }) {
                Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionLabel("Target times")
            }
            items(entries, key = { it.id }) { entry ->
                ScheduleEntryRow(
                    entry = entry,
                    onDelete = { entries = entries.filter { it.id != entry.id } }
                )
            }
            item {
                Text(
                    text = "+ Add time",
                    color = OchreColors.Accent,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { showAddEntry = true }
                )
            }
            item {
                SectionLabel("Max gap between walks")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxGapText,
                        onValueChange = { maxGapText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = settingsFieldColors(),
                        modifier = Modifier.width(80.dp)
                    )
                    Text("hours", color = OchreColors.TextSecondary, fontSize = 14.sp)
                }
            }
            item {
                SectionLabel("Overnight quiet hours")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("From", color = OchreColors.TextSecondary, fontSize = 14.sp)
                    OutlinedTextField(
                        value = quietFrom,
                        onValueChange = { quietFrom = it },
                        singleLine = true,
                        placeholder = { Text("22:00", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                        colors = settingsFieldColors(),
                        modifier = Modifier.width(80.dp)
                    )
                    Text("to", color = OchreColors.TextSecondary, fontSize = 14.sp)
                    OutlinedTextField(
                        value = quietTo,
                        onValueChange = { quietTo = it },
                        singleLine = true,
                        placeholder = { Text("07:00", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                        colors = settingsFieldColors(),
                        modifier = Modifier.width(80.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "No walk alerts during these hours",
                    color = OchreColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showAddEntry) {
        AddScheduleEntryDialog(
            onConfirm = { entry ->
                entries = entries + entry.copy(id = System.currentTimeMillis())
                showAddEntry = false
            },
            onDismiss = { showAddEntry = false }
        )
    }
}

@Composable
private fun ScheduleEntryRow(entry: WalkScheduleEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(entry.label, color = OchreColors.TextPrimary, fontSize = 15.sp)
            Text(
                "%02d:%02d  ±%d min".format(entry.targetHour, entry.targetMinute, entry.toleranceMinutes),
                color = OchreColors.TextSecondary, fontSize = 12.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OchreColors.Destructive)
        }
    }
}

@Composable
private fun AddScheduleEntryDialog(
    onConfirm: (WalkScheduleEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var tolerance by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Add walk time", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g. Morning", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    label = { Text("Label", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = settingsFieldColors()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    placeholder = { Text("HH:MM", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    label = { Text("Time", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = settingsFieldColors()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tolerance,
                        onValueChange = { tolerance = it },
                        label = { Text("±  min", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = settingsFieldColors(),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parts = time.split(":").mapNotNull { it.toIntOrNull() }
                if (label.isNotBlank() && parts.size == 2) {
                    onConfirm(
                        WalkScheduleEntry(
                            label = label,
                            targetHour = parts[0],
                            targetMinute = parts[1],
                            toleranceMinutes = tolerance.toIntOrNull() ?: 30
                        )
                    )
                }
            }) { Text("Add", color = OchreColors.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = OchreColors.TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor = OchreColors.TextPrimary,
    unfocusedTextColor = OchreColors.TextPrimary,
    cursorColor = OchreColors.Accent
)
