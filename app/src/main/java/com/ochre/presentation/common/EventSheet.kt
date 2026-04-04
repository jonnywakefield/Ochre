package com.ochre.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared bottom sheet used for:
 *  - Logging a new event (with optional value/note)
 *  - Editing an existing event (pre-filled)
 *  - Logging a past event (type picker + datetime)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSheet(
    // If non-null, we're editing an existing event
    existingEvent: DogEvent? = null,
    // If non-null, the type is pre-selected (no picker shown)
    preselectedType: EventType? = null,
    onConfirm: (type: EventType, timestampMillis: Long, value: Float?, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialType = existingEvent?.type ?: preselectedType ?: EventType.WALK
    val initialTimestamp = existingEvent?.timestampMillis ?: System.currentTimeMillis()
    val initialValue = existingEvent?.value?.toString() ?: ""
    val initialNote = existingEvent?.note ?: ""

    var selectedType by remember { mutableStateOf(initialType) }
    var valueText by remember { mutableStateOf(initialValue) }
    var noteText by remember { mutableStateOf(initialNote) }
    var selectedTimestamp by remember { mutableLongStateOf(initialTimestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val showTypePicker = preselectedType == null && existingEvent == null
    val needsValue = selectedType == EventType.WEIGHT || selectedType == EventType.FEED
    val needsNote = selectedType == EventType.MEDICAL ||
            selectedType == EventType.TRAINING ||
            selectedType == EventType.NOTE

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { datePart ->
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = datePart
                            set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                        }
                        selectedTimestamp = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK", color = OchreColors.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = OchreColors.TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = OchreColors.Surface,
                titleContentColor = OchreColors.TextPrimary,
                headlineContentColor = OchreColors.TextPrimary,
                weekdayContentColor = OchreColors.TextSecondary,
                selectedDayContainerColor = OchreColors.Accent,
                todayDateBorderColor = OchreColors.Accent
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = selectedTimestamp
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    selectedTimestamp = newCal.timeInMillis
                    showTimePicker = false
                }) { Text("OK", color = OchreColors.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = OchreColors.TextSecondary)
                }
            },
            containerColor = OchreColors.Surface,
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = OchreColors.Background,
                        clockDialSelectedContentColor = OchreColors.Background,
                        clockDialUnselectedContentColor = OchreColors.TextPrimary,
                        selectorColor = OchreColors.Accent,
                        containerColor = OchreColors.Surface,
                        timeSelectorSelectedContainerColor = OchreColors.Accent,
                        timeSelectorUnselectedContainerColor = OchreColors.Background,
                        timeSelectorSelectedContentColor = OchreColors.Background,
                        timeSelectorUnselectedContentColor = OchreColors.TextPrimary
                    )
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OchreColors.Surface, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .background(OchreColors.TextSecondary, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )

        // Header
        Text(
            text = when {
                existingEvent != null -> "edit entry"
                preselectedType != null -> preselectedType.name.lowercase()
                else -> "log event"
            },
            color = OchreColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        // Type picker (only when no type is pre-set)
        if (showTypePicker) {
            SheetLabel("type")
            EventTypeGrid(
                selected = selectedType,
                onSelect = { selectedType = it }
            )
        }

        // Datetime row
        SheetLabel("when")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetChip(
                text = dateFormatter.format(Date(selectedTimestamp)),
                onClick = { showDatePicker = true }
            )
            SheetChip(
                text = timeFormatter.format(Date(selectedTimestamp)),
                onClick = { showTimePicker = true }
            )
        }

        // Value input
        if (needsValue) {
            SheetLabel(if (selectedType == EventType.WEIGHT) "weight (kg)" else "amount (g)")
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OchreColors.Accent,
                    unfocusedBorderColor = OchreColors.TextSecondary,
                    focusedTextColor = OchreColors.TextPrimary,
                    unfocusedTextColor = OchreColors.TextPrimary,
                    cursorColor = OchreColors.Accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Note input
        if (needsNote) {
            SheetLabel("note")
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OchreColors.Accent,
                    unfocusedBorderColor = OchreColors.TextSecondary,
                    focusedTextColor = OchreColors.TextPrimary,
                    unfocusedTextColor = OchreColors.TextPrimary,
                    cursorColor = OchreColors.Accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("cancel", color = OchreColors.TextSecondary, fontSize = 15.sp)
            }
            Button(
                onClick = {
                    val parsedValue = valueText.toFloatOrNull()
                    val parsedNote = noteText.trim().ifEmpty { null }
                    onConfirm(selectedType, selectedTimestamp, parsedValue, parsedNote)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OchreColors.Accent,
                    contentColor = OchreColors.Background
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (existingEvent != null) "save" else "log",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        color = OchreColors.TextSecondary,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SheetChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(OchreColors.Background, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = OchreColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EventTypeGrid(selected: EventType, onSelect: (EventType) -> Unit) {
    val types = EventType.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    val isSelected = type == selected
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) OchreColors.Accent else OchreColors.Background,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelect(type) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) OchreColors.Background else OchreColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
