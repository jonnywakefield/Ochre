package com.ochre.presentation.walk

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.domain.model.WalkSession
import com.ochre.presentation.common.OchreColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WalkScreen(
    viewModel: WalkViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.isWalkActive) {
        ActiveWalkScreen(
            walk = uiState.activeWalk!!,
            onPoo = { viewModel.recordPoo() },
            onPee = { viewModel.recordPee() },
            onRemovePoo = { ts -> viewModel.removePoo(ts) },
            onRemovePee = { ts -> viewModel.removePee(ts) },
            onEnd = { viewModel.endWalk(context) }
        )
    } else {
        WalkHistoryScreen(
            history = uiState.history,
            onPoo = { viewModel.recordPoo() },
            onPee = { viewModel.recordPee() },
            onStartWalk = { viewModel.startWalk(context) },
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun ActiveWalkScreen(
    walk: WalkSession,
    onPoo: () -> Unit,
    onPee: () -> Unit,
    onRemovePoo: (Long) -> Unit,
    onRemovePee: (Long) -> Unit,
    onEnd: () -> Unit
) {
    var elapsedMillis by remember { mutableLongStateOf(System.currentTimeMillis() - walk.startMillis) }
    val haptic = LocalHapticFeedback.current

    // Flash state: 0=none, 1=poo flash, 2=pee flash
    var flashState by remember { mutableIntStateOf(0) }
    val flashColor by animateColorAsState(
        targetValue = when (flashState) {
            1 -> OchreColors.Accent.copy(alpha = 0.18f)
            2 -> Color(0xFF4A8FA8).copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300),
        label = "flash"
    )

    LaunchedEffect(walk.id) {
        while (true) {
            elapsedMillis = System.currentTimeMillis() - walk.startMillis
            kotlinx.coroutines.delay(1_000)
        }
    }

    LaunchedEffect(flashState) {
        if (flashState != 0) {
            kotlinx.coroutines.delay(600)
            flashState = 0
        }
    }

    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    // Combine poo + pee into a sorted list for display
    val events = remember(walk.pooEvents, walk.peeEvents) {
        (walk.pooEvents.map { Triple("Poo", it, false) } +
         walk.peeEvents.map { Triple("Pee", it, true) })
            .sortedBy { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        item {
            Spacer(Modifier.height(52.dp))
            Text(
                text = "Walk",
                color = OchreColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(flashColor, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatElapsed(elapsedMillis),
                    color = OchreColors.TextPrimary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )
            }
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WalkActionButton(label = "Poo", filled = false, modifier = Modifier.weight(1f), onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    flashState = 1
                    onPoo()
                })
                WalkActionButton(label = "Pee", filled = false, modifier = Modifier.weight(1f), onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    flashState = 2
                    onPee()
                })
            }
            Spacer(Modifier.height(12.dp))
            WalkActionButton(
                label = "End walk",
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onEnd
            )
        }

        if (events.isNotEmpty()) {
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "This walk",
                    color = OchreColors.TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            items(events, key = { "${it.first}_${it.second}" }) { (label, ts, isPee) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fmt.format(Date(ts))}  $label",
                        color = OchreColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "remove",
                        color = OchreColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            if (isPee) onRemovePee(ts) else onRemovePoo(ts)
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun WalkHistoryScreen(
    history: List<WalkSession>,
    onPoo: () -> Unit,
    onPee: () -> Unit,
    onStartWalk: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 52.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Walk",
                color = OchreColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Walk schedule",
                    tint = OchreColors.TextSecondary
                )
            }
        }

        // Start walk
        WalkActionButton(
            label = "Start walk",
            filled = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
            onClick = onStartWalk
        )

        // Quick-log poo/pee without an active walk
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WalkActionButton(
                label = "Log poo",
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPoo() }
            )
            WalkActionButton(
                label = "Log pee",
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onPee() }
            )
        }

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No walks yet", color = OchreColors.TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp)) {
                items(history.filter { !it.isActive }) { walk ->
                    WalkHistoryRow(walk)
                }
            }
        }
    }
}

@Composable
private fun WalkActionButton(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OchreColors.Accent,
                contentColor = OchreColors.Background
            )
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OchreColors.TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, OchreColors.TextSecondary)
        ) {
            Text(label, fontSize = 15.sp)
        }
    }
}

@Composable
private fun WalkHistoryRow(walk: WalkSession) {
    val dateFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val duration = walk.durationMillis?.let {
        val mins = TimeUnit.MILLISECONDS.toMinutes(it)
        "${mins}m"
    } ?: "ongoing"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dateFmt.format(Date(walk.startMillis)),
                color = OchreColors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = timeFmt.format(Date(walk.startMillis)),
                color = OchreColors.TextPrimary,
                fontSize = 15.sp
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(duration, color = OchreColors.TextSecondary, fontSize = 13.sp)
            val tags = buildList {
                if (walk.pooEvents.isNotEmpty()) add("poo ×${walk.pooEvents.size}")
                if (walk.peeEvents.isNotEmpty()) add("pee ×${walk.peeEvents.size}")
            }
            if (tags.isNotEmpty()) {
                Text(tags.joinToString("  "), color = OchreColors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
           else "%d:%02d".format(minutes, seconds)
}
