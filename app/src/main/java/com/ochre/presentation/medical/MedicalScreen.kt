package com.ochre.presentation.medical

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.domain.model.WeightEntry
import com.ochre.presentation.common.OchreColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicalScreen(viewModel: MedicalViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddWeight by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        item("header") {
            Spacer(Modifier.height(52.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Medical",
                    color = OchreColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp
                )
                Text(
                    "+ Log weight",
                    color = OchreColors.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { showAddWeight = true }
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        if (uiState.entries.size >= 2) {
            item("chart") {
                WeightChart(entries = uiState.entries.sortedBy { it.timestampMillis })
                Spacer(Modifier.height(24.dp))
            }
        }

        if (uiState.entries.isEmpty()) {
            item("empty") {
                Text("No weight entries yet", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            item("list_header") {
                Text(
                    "History",
                    color = OchreColors.TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.entries, key = { "weight_${it.id}" }) { entry ->
                WeightRow(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
            }
        }

        item("footer") { Spacer(Modifier.height(32.dp)) }
    }

    if (showAddWeight) {
        AddWeightDialog(
            onConfirm = { kg, ts, note ->
                viewModel.logWeight(kg, ts, note)
                showAddWeight = false
            },
            onDismiss = { showAddWeight = false }
        )
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>) {
    val accent = OchreColors.Accent
    val grid = OchreColors.TextSecondary.copy(alpha = 0.2f)

    val minWeight = entries.minOf { it.weightKg }
    val maxWeight = entries.maxOf { it.weightKg }
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)
    val minTime = entries.first().timestampMillis
    val maxTime = entries.last().timestampMillis
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)

    val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val padLeft = 0f
            val padRight = 0f
            val padTop = 12f
            val padBottom = 20f
            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padTop - padBottom

            // Grid lines (3 horizontal)
            for (i in 0..2) {
                val y = padTop + chartH * (1f - i / 2f)
                drawLine(color = grid, start = Offset(padLeft, y), end = Offset(padLeft + chartW, y), strokeWidth = 1f)
            }

            fun xOf(ts: Long) = padLeft + chartW * ((ts - minTime).toFloat() / timeRange)
            fun yOf(kg: Float) = padTop + chartH * (1f - (kg - minWeight) / weightRange)

            // Line path
            val path = Path()
            entries.forEachIndexed { i, entry ->
                val x = xOf(entry.timestampMillis)
                val y = yOf(entry.weightKg)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = accent, style = Stroke(width = 2.5f))

            // Dots
            entries.forEach { entry ->
                drawCircle(color = accent, radius = 5f, center = Offset(xOf(entry.timestampMillis), yOf(entry.weightKg)))
                drawCircle(color = Color(0xFF1A1209), radius = 3f, center = Offset(xOf(entry.timestampMillis), yOf(entry.weightKg)))
            }
        }

        // X-axis labels — show first, last, maybe middle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateFmt.format(Date(entries.first().timestampMillis)), color = OchreColors.TextSecondary, fontSize = 10.sp)
            if (entries.size > 2) {
                val mid = entries[entries.size / 2]
                Text(dateFmt.format(Date(mid.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 10.sp)
            }
            Text(dateFmt.format(Date(entries.last().timestampMillis)), color = OchreColors.TextSecondary, fontSize = 10.sp)
        }

        Spacer(Modifier.height(8.dp))

        // Weight range label
        val latestKg = entries.last().weightKg
        Text(
            "Latest: ${"%.1f".format(latestKg)} kg",
            color = OchreColors.TextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun WeightRow(entry: WeightEntry, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("EEE d MMM  HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("${"%.1f".format(entry.weightKg)} kg", color = OchreColors.TextPrimary, fontSize = 15.sp)
            if (entry.note.isNotBlank()) {
                Text(entry.note, color = OchreColors.TextSecondary, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(fmt.format(Date(entry.timestampMillis)), color = OchreColors.TextSecondary, fontSize = 12.sp)
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = OchreColors.TextSecondary,
                modifier = Modifier.size(16.dp).clickable { onDelete() }
            )
        }
    }
}

@Composable
private fun AddWeightDialog(
    onConfirm: (Float, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var kgText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OchreColors.Surface,
        title = { Text("Log weight", color = OchreColors.TextPrimary, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = kgText,
                    onValueChange = { kgText = it; error = null },
                    label = { Text("Weight (kg)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. 12.4", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = weightFieldColors()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = weightFieldColors()
                )
                if (error != null) {
                    Text(error!!, color = OchreColors.Destructive, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kg = kgText.toFloatOrNull()
                when {
                    kg == null -> error = "Enter a valid number"
                    kg < 0.5f || kg > 120f -> error = "Weight must be 0.5–120 kg"
                    else -> onConfirm(kg, System.currentTimeMillis(), noteText.trim())
                }
            }) {
                Text("Save", color = OchreColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OchreColors.TextSecondary) }
        }
    )
}

@Composable
private fun weightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor = OchreColors.TextPrimary,
    unfocusedTextColor = OchreColors.TextPrimary,
    cursorColor = OchreColors.Accent
)
