package com.ochre.presentation.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ochre.presentation.common.OchreColors

@Composable
fun TrainingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column {
            Spacer(Modifier.height(52.dp))
            Text(
                "Training",
                color = OchreColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "Coming soon",
                color = OchreColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
