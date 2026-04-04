package com.ochre.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.ochre.presentation.common.OchreColors
import com.ochre.service.NotificationPrefs

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { NotificationPrefs.get(context) }

    var aloneLimit by remember { mutableStateOf(prefs.aloneMaxMinutes.toString()) }
    var aloneLimitSaved by remember { mutableStateOf(true) }

    var barStart by remember { mutableStateOf(prefs.barStartHour.toString()) }
    var barEnd   by remember { mutableStateOf(prefs.barEndHour.toString()) }
    var barRangeSaved by remember { mutableStateOf(true) }

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

        Spacer(Modifier.height(36.dp))
        SettingsSectionLabel("Alone limit")
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Maximum time away before the notification turns red",
            color = OchreColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = aloneLimit,
                onValueChange = { v ->
                    aloneLimit = v.filter { it.isDigit() }.take(3)
                    aloneLimitSaved = false
                },
                label = { Text("Minutes", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp),
                colors = outlinedFieldColors()
            )
            if (!aloneLimitSaved) {
                TextButton(onClick = {
                    val v = aloneLimit.toIntOrNull()?.coerceIn(10, 720)
                    if (v != null) {
                        aloneLimit = v.toString()
                        NotificationPrefs.set(context, NotificationPrefs.KEY_ALONE_MAX_MINUTES, v)
                        aloneLimitSaved = true
                    }
                }) {
                    Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
                }
            } else {
                Text("${aloneLimit}m", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(36.dp))
        SettingsSectionLabel("Notification bar range")
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Hours shown on the 24-hour progress bar (0–23)",
            color = OchreColors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = barStart,
                onValueChange = { v ->
                    barStart = v.filter { it.isDigit() }.take(2)
                    barRangeSaved = false
                },
                label = { Text("From", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(90.dp),
                colors = outlinedFieldColors()
            )
            Text("to", color = OchreColors.TextSecondary, fontSize = 13.sp)
            OutlinedTextField(
                value = barEnd,
                onValueChange = { v ->
                    barEnd = v.filter { it.isDigit() }.take(2)
                    barRangeSaved = false
                },
                label = { Text("To", color = OchreColors.TextSecondary, fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(90.dp),
                colors = outlinedFieldColors()
            )
            if (!barRangeSaved) {
                TextButton(onClick = {
                    val s = barStart.toIntOrNull()?.coerceIn(0, 22)
                    val e = barEnd.toIntOrNull()?.coerceIn(1, 23)
                    if (s != null && e != null && e > s) {
                        barStart = s.toString()
                        barEnd   = e.toString()
                        NotificationPrefs.set(context, NotificationPrefs.KEY_BAR_START_HOUR, s)
                        NotificationPrefs.set(context, NotificationPrefs.KEY_BAR_END_HOUR, e)
                        barRangeSaved = true
                    }
                }) {
                    Text("Save", color = OchreColors.Accent, fontSize = 14.sp)
                }
            } else {
                Text("${barStart}:00 – ${barEnd}:00", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

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
