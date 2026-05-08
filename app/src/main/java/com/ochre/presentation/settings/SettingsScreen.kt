package com.ochre.presentation.settings

import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.ochre.R
import com.ochre.domain.usecase.food.GetMealScheduleUseCase
import com.ochre.presentation.common.OchreColors
import com.ochre.service.NotificationPrefs
import com.ochre.service.OchreNotificationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(getMealScheduleUseCase: GetMealScheduleUseCase) {
    val context = LocalContext.current
    val prefs = remember { NotificationPrefs.get(context) }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(52.dp))
        Text(
            text = "settings",
            color = OchreColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(24.dp))

        // Tab row
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf("General", "Testing").forEachIndexed { index, label ->
                val active = selectedTab == index
                TextButton(
                    onClick = { selectedTab = index },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label.lowercase(),
                        color = if (active) OchreColors.Accent else OchreColors.TextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> GeneralTab(context = context, prefs = prefs)
            1 -> TestingTab(context = context, getMealScheduleUseCase = getMealScheduleUseCase)
        }
    }
}

@Composable
private fun GeneralTab(context: Context, prefs: NotificationPrefs.Prefs) {
    // Alone
    var aloneEnabled by remember { mutableStateOf(prefs.aloneEnabled) }
    var aloneLimit by remember { mutableStateOf(prefs.aloneMaxMinutes.toString()) }
    var aloneLimitSaved by remember { mutableStateOf(true) }
    var aloneRepeat by remember { mutableStateOf(prefs.aloneRepeatMinutes.toString()) }
    var aloneRepeatSaved by remember { mutableStateOf(true) }

    // Food
    var foodEnabled by remember { mutableStateOf(prefs.foodEnabled) }
    var foodRepeat by remember { mutableStateOf(prefs.foodRepeatMinutes.toString()) }
    var foodRepeatSaved by remember { mutableStateOf(true) }

    // Walk gap
    var walkGapEnabled by remember { mutableStateOf(prefs.walkGapEnabled) }
    var walkLimit by remember { mutableStateOf(prefs.walkLimitMinutes.toString()) }
    var walkLimitSaved by remember { mutableStateOf(true) }
    var walkGapRepeat by remember { mutableStateOf(prefs.walkGapRepeatMinutes.toString()) }
    var walkGapRepeatSaved by remember { mutableStateOf(true) }

    // Walk scheduled times
    var walkSchedEnabled by remember { mutableStateOf(prefs.walkSchedEnabled) }
    var walkRecent by remember { mutableStateOf(prefs.walkRecentMinutes.toString()) }
    var walkRecentSaved by remember { mutableStateOf(true) }

    // Bar range
    var barStart by remember { mutableStateOf(minutesToHHMM(prefs.barStartMinute)) }
    var barEnd   by remember { mutableStateOf(minutesToHHMM(prefs.barEndMinute)) }
    var barRangeSaved by remember { mutableStateOf(true) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))

        // ── Alone ─────────────────────────────────────────────────────────────
        SettingsSectionLabel("Alone")
        Spacer(Modifier.height(8.dp))
        EnabledRow(label = "Alone alert", enabled = aloneEnabled) {
            aloneEnabled = it
            NotificationPrefs.set(context, NotificationPrefs.KEY_ALONE_ENABLED, it)
        }
        if (aloneEnabled) {
            Spacer(Modifier.height(8.dp))
            Text("Limit", color = OchreColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            MinutesRow(value = aloneLimit, saved = aloneLimitSaved, suffix = "m",
                onChanged = { aloneLimit = it; aloneLimitSaved = false },
                onSave = {
                    val v = aloneLimit.toIntOrNull()?.coerceIn(10, 720) ?: return@MinutesRow
                    aloneLimit = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_ALONE_MAX_MINUTES, v)
                    aloneLimitSaved = true
                }
            )
            Spacer(Modifier.height(8.dp))
            RepeatRow(label = "Repeat", value = aloneRepeat, saved = aloneRepeatSaved,
                onChanged = { aloneRepeat = it; aloneRepeatSaved = false },
                onSave = {
                    val v = aloneRepeat.toIntOrNull()?.coerceIn(0, 120) ?: return@RepeatRow
                    aloneRepeat = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_ALONE_REPEAT_MINUTES, v)
                    aloneRepeatSaved = true
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Food ──────────────────────────────────────────────────────────────
        SettingsSectionLabel("Food")
        Spacer(Modifier.height(8.dp))
        EnabledRow(label = "Meal reminders", enabled = foodEnabled) {
            foodEnabled = it
            NotificationPrefs.set(context, NotificationPrefs.KEY_FOOD_ENABLED, it)
        }
        if (foodEnabled) {
            Spacer(Modifier.height(8.dp))
            RepeatRow(label = "Repeat", value = foodRepeat, saved = foodRepeatSaved,
                onChanged = { foodRepeat = it; foodRepeatSaved = false },
                onSave = {
                    val v = foodRepeat.toIntOrNull()?.coerceIn(0, 120) ?: return@RepeatRow
                    foodRepeat = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_FOOD_REPEAT_MINUTES, v)
                    foodRepeatSaved = true
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Walk gap ──────────────────────────────────────────────────────────
        SettingsSectionLabel("Walk gap")
        Spacer(Modifier.height(8.dp))
        EnabledRow(label = "Gap alert", enabled = walkGapEnabled) {
            walkGapEnabled = it
            NotificationPrefs.set(context, NotificationPrefs.KEY_WALK_GAP_ENABLED, it)
        }
        if (walkGapEnabled) {
            Spacer(Modifier.height(8.dp))
            Text("Alert after", color = OchreColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            MinutesRow(value = walkLimit, saved = walkLimitSaved, suffix = "m",
                onChanged = { walkLimit = it; walkLimitSaved = false },
                onSave = {
                    val v = walkLimit.toIntOrNull()?.coerceIn(30, 1440) ?: return@MinutesRow
                    walkLimit = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_WALK_LIMIT_MINUTES, v)
                    walkLimitSaved = true
                }
            )
            Spacer(Modifier.height(8.dp))
            RepeatRow(label = "Repeat", value = walkGapRepeat, saved = walkGapRepeatSaved,
                onChanged = { walkGapRepeat = it; walkGapRepeatSaved = false },
                onSave = {
                    val v = walkGapRepeat.toIntOrNull()?.coerceIn(0, 120) ?: return@RepeatRow
                    walkGapRepeat = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_WALK_GAP_REPEAT_MINUTES, v)
                    walkGapRepeatSaved = true
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Walk scheduled times ──────────────────────────────────────────────
        SettingsSectionLabel("Walk schedule")
        Spacer(Modifier.height(8.dp))
        EnabledRow(label = "Scheduled time alerts", enabled = walkSchedEnabled) {
            walkSchedEnabled = it
            NotificationPrefs.set(context, NotificationPrefs.KEY_WALK_SCHED_ENABLED, it)
        }
        if (walkSchedEnabled) {
            Spacer(Modifier.height(8.dp))
            Text("Skip if walked within", color = OchreColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            MinutesRow(value = walkRecent, saved = walkRecentSaved, suffix = "m",
                onChanged = { walkRecent = it; walkRecentSaved = false },
                onSave = {
                    val v = walkRecent.toIntOrNull()?.coerceIn(0, 480) ?: return@MinutesRow
                    walkRecent = v.toString()
                    NotificationPrefs.set(context, NotificationPrefs.KEY_WALK_RECENT_MINUTES, v)
                    walkRecentSaved = true
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Notification bar range ────────────────────────────────────────────
        SettingsSectionLabel("Notification bar range")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Start and end time shown on the progress bar (HH:MM)",
            color = OchreColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = barStart,
                onValueChange = { v ->
                    barStart = v.filter { it.isDigit() || it == ':' }.take(5)
                    barRangeSaved = false
                },
                label = { Text("From", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                placeholder = { Text("06:00", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
                colors = outlinedFieldColors()
            )
            Text("to", color = OchreColors.TextSecondary, fontSize = 13.sp)
            OutlinedTextField(
                value = barEnd,
                onValueChange = { v ->
                    barEnd = v.filter { it.isDigit() || it == ':' }.take(5)
                    barRangeSaved = false
                },
                label = { Text("To", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                placeholder = { Text("23:00", color = OchreColors.TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
                colors = outlinedFieldColors()
            )
            if (!barRangeSaved) {
                TextButton(onClick = {
                    val s = parseHHMM(barStart)
                    val e = parseHHMM(barEnd)
                    if (s != null && e != null && e > s) {
                        barStart = minutesToHHMM(s)
                        barEnd   = minutesToHHMM(e)
                        NotificationPrefs.set(context, NotificationPrefs.KEY_BAR_START_MINUTE, s)
                        NotificationPrefs.set(context, NotificationPrefs.KEY_BAR_END_MINUTE, e)
                        barRangeSaved = true
                    }
                }) {
                    Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
                }
            } else {
                Text("$barStart – $barEnd", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TestingTab(context: Context, getMealScheduleUseCase: GetMealScheduleUseCase) {
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Spacer(Modifier.height(12.dp))
        SettingsSectionLabel("Notifications")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Fire test notifications through the same channels as the real ones.",
            color = OchreColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Food — meal window alert
        TestRow(label = "Meal reminder", description = "Food alert channel — uses first configured meal") {
            scope.launch {
                val meals = getMealScheduleUseCase().first()
                val meal = meals.firstOrNull()
                val title = if (meal != null) "09:00 ${meal.label}" else "09:00 breakfast"
                fireNotification(context, OchreNotificationManager.CHANNEL_ALERTS, 9001, title, "")
            }
        }

        Spacer(Modifier.height(4.dp))

        // Reminders channel
        TestRow(label = "Reminder", description = "Reminders channel (vet, treatment, etc.)") {
            fireNotification(context, OchreNotificationManager.CHANNEL_REMINDERS, 9002, "vet 14:00", "")
        }

        Spacer(Modifier.height(4.dp))

        // Walk gap alert
        TestRow(label = "Walk gap alert", description = "Alert channel heads-up — walk gap exceeded") {
            fireNotification(context, OchreNotificationManager.CHANNEL_ALERTS, 9003, "no walk 6h0m", "")
        }

        Spacer(Modifier.height(4.dp))

        TestRow(label = "Alone alert", description = "Alert channel heads-up — alone limit exceeded") {
            fireNotification(context, OchreNotificationManager.CHANNEL_ALERTS, 9004, "4h30m alone", "")
        }
    }
}

private fun fireNotification(context: Context, channel: String, id: Int, title: String, text: String) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val isAlert = channel == OchreNotificationManager.CHANNEL_ALERTS
    val notification = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_paw)
        .setColor(0xFFE4A853.toInt())
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(if (isAlert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        .apply { if (isAlert) setDefaults(NotificationCompat.DEFAULT_ALL) }
        .setAutoCancel(true)
        .build()
    nm.notify(id, notification)
}

@Composable
private fun TestRow(label: String, description: String, onFire: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = OchreColors.TextPrimary, fontSize = 14.sp)
            Text(description, color = OchreColors.TextSecondary, fontSize = 11.sp)
        }
        TextButton(onClick = onFire) {
            Text("Fire", color = OchreColors.Accent, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EnabledRow(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OchreColors.TextPrimary, fontSize = 14.sp)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OchreColors.Accent,
                checkedTrackColor = OchreColors.Accent.copy(alpha = 0.4f),
                uncheckedThumbColor = OchreColors.TextSecondary,
                uncheckedTrackColor = OchreColors.TextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun MinutesRow(
    value: String,
    saved: Boolean,
    suffix: String,
    onChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChanged(it.filter { c -> c.isDigit() }.take(4)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(100.dp),
            colors = outlinedFieldColors()
        )
        if (!saved) {
            TextButton(onClick = onSave) {
                Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
            }
        } else {
            Text("$value$suffix", color = OchreColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RepeatRow(
    label: String,
    value: String,
    saved: Boolean,
    onChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OchreColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.width(60.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { onChanged(it.filter { c -> c.isDigit() }.take(3)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                colors = outlinedFieldColors()
            )
            if (!saved) {
                TextButton(onClick = onSave) {
                    Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
                }
            } else {
                Text(if (value == "0") "off" else "${value}m", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

/** Parse "HH:MM" or plain "H" into total minutes, returns null on failure. */
private fun parseHHMM(input: String): Int? {
    val trimmed = input.trim()
    return if (trimmed.contains(':')) {
        val parts = trimmed.split(':')
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        h * 60 + m
    } else {
        val h = trimmed.toIntOrNull() ?: return null
        if (h !in 0..23) return null
        h * 60
    }
}

private fun minutesToHHMM(totalMinutes: Int): String =
    "%02d:%02d".format(totalMinutes / 60, totalMinutes % 60)

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = OchreColors.Accent,
    unfocusedBorderColor = OchreColors.TextSecondary,
    focusedTextColor     = OchreColors.TextPrimary,
    unfocusedTextColor   = OchreColors.TextPrimary,
    cursorColor          = OchreColors.Accent
)

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = OchreColors.TextSecondary,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.Medium
    )
}
