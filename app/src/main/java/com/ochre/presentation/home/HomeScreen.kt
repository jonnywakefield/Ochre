package com.ochre.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.presentation.common.OchreColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showNoteSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(52.dp))

        Text(
            text = "Ochre",
            color = OchreColors.Accent,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(28.dp))

        // Status dashboard
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
            val walkLabel = when {
                activeWalk != null  -> "Walking  ${formatElapsed(now - activeWalk.startMillis)}"
                lastWalkEnd != null -> "Last walk  ${formatElapsed(now - lastWalkEnd)} ago"
                else                -> "Last walk  —"
            }
            DashRow(
                label = walkLabel,
                value = null,
                highlight = uiState.isWalkActive
            )

            HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)

            val lastFed = uiState.lastFedMillis
            val nextMeal = uiState.nextMeal
            val nextMealMinutes = uiState.nextMealMinutes
            val feedValue = when {
                nextMeal != null && nextMealMinutes != null ->
                    if (nextMealMinutes < 60) "next in ${nextMealMinutes}m"
                    else "next in ${nextMealMinutes / 60}h ${nextMealMinutes % 60}m"
                else -> null
            }
            DashRow(
                label = if (lastFed != null) "Last fed  ${formatElapsed(now - lastFed)} ago" else "Last fed  —",
                value = feedValue
            )

            val stockGrams = uiState.stockGrams
            if (stockGrams > 0) {
                HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)
                DashRow(
                    label = "Stock  ${"%,d".format(stockGrams)}g",
                    value = null,
                    warn = stockGrams < 500
                )
            }

            val activeAlone = uiState.activeAlone
            if (activeAlone != null) {
                HorizontalDivider(color = OchreColors.Accent.copy(alpha = 0.08f), thickness = 0.5.dp)
                DashRow(
                    label = "Away  ${formatElapsed(now - activeAlone.startMillis)}",
                    value = null,
                    highlight = true
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Walk — filled accent when active, filled-dim when inactive
            Button(
                onClick = {
                    if (uiState.isWalkActive) viewModel.endWalk(context)
                    else viewModel.startWalk(context)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isWalkActive) OchreColors.Accent else Color(0xFF2A2218),
                    contentColor = if (uiState.isWalkActive) OchreColors.Background else OchreColors.Accent
                )
            ) {
                Text(
                    if (uiState.isWalkActive) "End walk" else "Start walk",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Leave / Back home
            Button(
                onClick = {
                    if (uiState.isAloneActive) viewModel.endAlone(context)
                    else viewModel.startAlone(context)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = OchreColors.TextPrimary
                )
            ) {
                Text(
                    if (uiState.isAloneActive) "Back home" else "Leave",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Note — subtle
            TextButton(
                onClick = { showNoteSheet = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Write note", fontSize = 14.sp, color = OchreColors.TextSecondary)
            }
        }
    }

    if (showNoteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNoteSheet = false },
            sheetState = sheetState,
            containerColor = OchreColors.Surface,
            dragHandle = null
        ) {
            NoteSheet(
                onConfirm = { note ->
                    viewModel.logNote(note)
                    showNoteSheet = false
                },
                onDismiss = { showNoteSheet = false }
            )
        }
    }
}

@Composable
private fun DashRow(
    label: String,
    value: String?,
    highlight: Boolean = false,
    warn: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = when {
                highlight -> OchreColors.Accent
                warn      -> OchreColors.Destructive
                else      -> OchreColors.TextPrimary
            },
            fontSize = 14.sp
        )
        if (value != null) {
            Text(
                text = value,
                color = OchreColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun NoteSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Note", color = OchreColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Light)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            placeholder = { Text("Write a note...", color = OchreColors.TextSecondary, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OchreColors.Accent,
                unfocusedBorderColor = OchreColors.TextSecondary,
                focusedTextColor = OchreColors.TextPrimary,
                unfocusedTextColor = OchreColors.TextPrimary,
                cursorColor = OchreColors.Accent
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OchreColors.TextSecondary, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun formatElapsed(millis: Long): String {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
