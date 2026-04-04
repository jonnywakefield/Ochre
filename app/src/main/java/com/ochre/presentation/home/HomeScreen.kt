package com.ochre.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Tick every 30s to refresh elapsed times
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
            text = "ochre",
            color = OchreColors.Accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(32.dp))

        // Status dashboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OchreColors.Surface, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Walk status
            val activeWalk = uiState.activeWalk
            val lastWalkEnd = uiState.lastWalkEndMillis
            val walkLabel = when {
                activeWalk != null  -> "Walk  ${formatElapsed(now - activeWalk.startMillis)}"
                lastWalkEnd != null -> "Last walk  ${formatElapsed(now - lastWalkEnd)} ago"
                else                -> "Walk  —"
            }
            DashRow(label = walkLabel, highlight = uiState.isWalkActive)

            // Feed status
            val lastFed = uiState.lastFedMillis
            val feedLabel = if (lastFed != null) "Last fed  ${formatElapsed(now - lastFed)} ago" else "Last fed  —"
            DashRow(label = feedLabel)

            // Next meal
            val nextMeal = uiState.nextMeal
            val nextMealMinutes = uiState.nextMealMinutes
            if (nextMeal != null && nextMealMinutes != null) {
                val nextLabel = if (nextMealMinutes < 60) "Next feed  in ${nextMealMinutes}m (${nextMeal.label})"
                               else "Next feed  in ${nextMealMinutes / 60}h ${nextMealMinutes % 60}m (${nextMeal.label})"
                DashRow(label = nextLabel)
            }

            // Stock
            if (uiState.stockGrams > 0) {
                DashRow(
                    label = "Stock  ${"%,d".format(uiState.stockGrams)}g",
                    warn = uiState.stockGrams < 500
                )
            }

            // Away status
            val activeAlone = uiState.activeAlone
            if (activeAlone != null) {
                DashRow(
                    label = "Away  ${formatElapsed(now - activeAlone.startMillis)}",
                    highlight = true
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeButton(
                label = if (uiState.isWalkActive) "End walk" else "Start walk",
                style = if (uiState.isWalkActive) ButtonStyle.FilledAccent else ButtonStyle.Outlined,
                onClick = {
                    if (uiState.isWalkActive) viewModel.endWalk(context)
                    else viewModel.startWalk(context)
                }
            )

            HomeButton(
                label = if (uiState.isAloneActive) "Back home" else "Leave",
                style = ButtonStyle.Outlined,
                onClick = {
                    if (uiState.isAloneActive) viewModel.endAlone(context)
                    else viewModel.startAlone(context)
                }
            )

            HomeButton(
                label = "Write note",
                style = ButtonStyle.Subtle,
                onClick = { showNoteSheet = true }
            )
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

private enum class ButtonStyle { FilledAccent, Outlined, Subtle }

@Composable
private fun DashRow(label: String, highlight: Boolean = false, warn: Boolean = false) {
    Text(
        text = label,
        color = when {
            highlight -> OchreColors.Accent
            warn -> OchreColors.Destructive
            else -> OchreColors.TextSecondary
        },
        fontSize = 13.sp
    )
}

@Composable
private fun HomeButton(
    label: String,
    style: ButtonStyle,
    onClick: () -> Unit
) {
    when (style) {
        ButtonStyle.FilledAccent -> Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OchreColors.Accent,
                contentColor = OchreColors.Background
            )
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        ButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OchreColors.TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, OchreColors.TextSecondary)
        ) {
            Text(label, fontSize = 15.sp)
        }

        ButtonStyle.Subtle -> TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(label, fontSize = 15.sp, color = OchreColors.TextSecondary)
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
