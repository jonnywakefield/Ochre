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

        Spacer(Modifier.height(48.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Walk button — filled when active, outlined when inactive
            HomeButton(
                label = if (uiState.isWalkActive) "End walk" else "Start walk",
                style = if (uiState.isWalkActive) ButtonStyle.FilledAccent else ButtonStyle.Outlined,
                onClick = {
                    if (uiState.isWalkActive) viewModel.endWalk(context)
                    else viewModel.startWalk(context)
                }
            )

            // Alone button
            HomeButton(
                label = if (uiState.isAloneActive) "Back home" else "Leave",
                style = ButtonStyle.Outlined,
                onClick = {
                    if (uiState.isAloneActive) viewModel.endAlone(context)
                    else viewModel.startAlone(context)
                }
            )

            // Note button
            HomeButton(
                label = "Write note",
                style = ButtonStyle.Subtle,
                onClick = { showNoteSheet = true }
            )
        }

        // Active state indicators
        if (uiState.isWalkActive || uiState.isAloneActive) {
            Spacer(Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (uiState.isWalkActive) {
                    StatusLine("Walk in progress")
                }
                if (uiState.isAloneActive) {
                    StatusLine("Away from home")
                }
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

private enum class ButtonStyle { FilledAccent, Outlined, Subtle }

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
private fun StatusLine(text: String) {
    Text(
        text = text,
        color = OchreColors.TextSecondary,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )
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
