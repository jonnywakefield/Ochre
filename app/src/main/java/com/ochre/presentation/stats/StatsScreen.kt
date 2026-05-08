package com.ochre.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ochre.presentation.common.OchreColors
import kotlin.math.roundToInt

private val ColorPee  = Color(0xFF6AABCC)
private val ColorPoo  = Color(0xFFB07040)
private val ColorWalk = Color(0xFFE4A853)
private val ColorFeed = Color(0xFF7EBD8A)

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OchreColors.Background),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        item("header") {
            Spacer(Modifier.height(52.dp))
            Text(
                "Stats",
                color = OchreColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(20.dp))
            PeriodSelector(selected = state.period, onSelect = viewModel::setPeriod)
            Spacer(Modifier.height(28.dp))
        }

        if (state.isLoading) {
            item("loading") {
                Text("Loading…", color = OchreColors.TextSecondary, fontSize = 13.sp)
            }
            return@LazyColumn
        }

        item("summary_cards") {
            SummaryCards(state)
            Spacer(Modifier.height(32.dp))
        }

        if (state.dailyToilet.isNotEmpty()) {
            item("toilet_title") {
                SectionTitle("Toilet frequency")
            }
            item("toilet_legend") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(ColorPee, "Pee")
                    LegendDot(ColorPoo, "Poo")
                }
                Spacer(Modifier.height(12.dp))
            }
            item("toilet_chart") {
                GroupedBarChart(
                    entries = state.dailyToilet,
                    colorA = ColorPee,
                    colorB = ColorPoo,
                    maxVal = (state.dailyToilet.maxOf { maxOf(it.peeCount, it.pooCount) }).coerceAtLeast(1)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("avg pee ${"%.1f".format(state.avgPeesPerDay)}/day", color = OchreColors.TextSecondary, fontSize = 11.sp)
                    Text("avg poo ${"%.1f".format(state.avgPoosPerDay)}/day", color = OchreColors.TextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (state.dailyWalks.any { it.walkCount > 0 }) {
            item("walk_title") { SectionTitle("Walk frequency") }
            item("walk_chart") {
                WalkBarChart(entries = state.dailyWalks)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${"%.1f".format(state.avgWalksPerDay)} walks/day", color = OchreColors.TextSecondary, fontSize = 11.sp)
                    if (state.avgWalkDurationMinutes > 0) {
                        Text("avg ${state.avgWalkDurationMinutes.roundToInt()} min", color = OchreColors.TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        item("routine_title") { SectionTitle("Daily routine (24h)") }
        item("routine_legend") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(ColorWalk, "Walk")
                LegendDot(ColorPee,  "Pee")
                LegendDot(ColorPoo,  "Poo")
                LegendDot(ColorFeed, "Feed")
            }
            Spacer(Modifier.height(12.dp))
        }
        item("routine_chart") {
            DailyRoutineChart(state.hourly)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("midnight", color = OchreColors.TextSecondary, fontSize = 10.sp)
                Text("noon", color = OchreColors.TextSecondary, fontSize = 10.sp)
                Text("midnight", color = OchreColors.TextSecondary, fontSize = 10.sp)
            }
            Spacer(Modifier.height(32.dp))
        }

        if (state.pooAfterFoodMinutes.isNotEmpty()) {
            item("paf_title") { SectionTitle("Poo timing after last meal") }
            item("paf_chart") {
                PooAfterFoodChart(state.pooAfterFoodMinutes)
                Spacer(Modifier.height(8.dp))
                state.avgPooAfterFoodMinutes?.let { avg ->
                    val h = (avg / 60).toInt()
                    val m = (avg % 60).roundToInt()
                    val label = if (h > 0) "${h}h ${m}m" else "${m}m"
                    Text("average: $label after meal", color = OchreColors.TextSecondary, fontSize = 11.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        item("footer") { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Period selector ──────────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsPeriod.entries.forEach { p ->
            val active = p == selected
            Box(
                modifier = Modifier
                    .background(
                        color = if (active) OchreColors.Accent.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelect(p) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    p.label,
                    color = if (active) OchreColors.Accent else OchreColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

// ── Summary cards ────────────────────────────────────────────────────────────

@Composable
private fun SummaryCards(state: StatsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), color = ColorPee,  label = "Total pees",  value = state.totalPees.toString())
            StatCard(modifier = Modifier.weight(1f), color = ColorPoo,  label = "Total poos",  value = state.totalPoos.toString())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(modifier = Modifier.weight(1f), color = ColorWalk, label = "Total walks", value = state.totalWalks.toString())
            StatCard(
                modifier = Modifier.weight(1f),
                color = ColorWalk,
                label = "Avg walk",
                value = if (state.avgWalkDurationMinutes > 0) "${state.avgWalkDurationMinutes.roundToInt()} min" else "—"
            )
        }
        state.avgPooAfterFoodMinutes?.let { avg ->
            val h = (avg / 60).toInt()
            val m = (avg % 60).roundToInt()
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorPoo,
                    label = "Avg poo after meal",
                    value = if (h > 0) "${h}h ${m}m" else "${m}m"
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, color: Color, label: String, value: String) {
    Box(
        modifier = modifier
            .background(OchreColors.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text(label, color = OchreColors.TextSecondary, fontSize = 11.sp, letterSpacing = 0.3.sp)
        }
    }
}

// ── Grouped bar chart (toilet) ────────────────────────────────────────────────

@Composable
private fun GroupedBarChart(
    entries: List<DailyToiletEntry>,
    colorA: Color,
    colorB: Color,
    maxVal: Int
) {
    val visible = if (entries.size > 30) entries.takeLast(30) else entries
    val grid = OchreColors.TextSecondary.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val padTop = 8f
        val padBottom = 20f
        val chartH = size.height - padTop - padBottom
        val n = visible.size
        if (n == 0) return@Canvas
        val slotW = size.width / n
        val barW = (slotW * 0.35f).coerceAtLeast(3f)
        val gap = barW * 0.2f

        // Grid lines
        for (i in 0..3) {
            val y = padTop + chartH * (1f - i / 3f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        visible.forEachIndexed { idx, entry ->
            val cx = slotW * idx + slotW / 2f

            val hA = chartH * (entry.peeCount.toFloat() / maxVal)
            val hB = chartH * (entry.pooCount.toFloat() / maxVal)

            if (entry.peeCount > 0) {
                drawRoundRect(
                    color = colorA,
                    topLeft = Offset(cx - barW - gap / 2, padTop + chartH - hA),
                    size = Size(barW, hA),
                    cornerRadius = CornerRadius(2f)
                )
            }
            if (entry.pooCount > 0) {
                drawRoundRect(
                    color = colorB,
                    topLeft = Offset(cx + gap / 2, padTop + chartH - hB),
                    size = Size(barW, hB),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }
    }

    // X-axis date labels (first, mid, last)
    if (visible.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(visible.first().label, color = OchreColors.TextSecondary, fontSize = 10.sp)
            if (visible.size > 2) Text(visible[visible.size / 2].label, color = OchreColors.TextSecondary, fontSize = 10.sp)
            Text(visible.last().label, color = OchreColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

// ── Walk bar chart ────────────────────────────────────────────────────────────

@Composable
private fun WalkBarChart(entries: List<DailyWalkEntry>) {
    val visible = if (entries.size > 30) entries.takeLast(30) else entries
    val maxCount = visible.maxOf { it.walkCount }.coerceAtLeast(1)
    val maxDur = visible.maxOf { it.avgDurationMinutes }.coerceAtLeast(1f)
    val grid = OchreColors.TextSecondary.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val padTop = 8f
        val padBottom = 20f
        val chartH = size.height - padTop - padBottom
        val n = visible.size
        if (n == 0) return@Canvas
        val slotW = size.width / n
        val barW = (slotW * 0.5f).coerceAtLeast(3f)

        for (i in 0..3) {
            val y = padTop + chartH * (1f - i / 3f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        visible.forEachIndexed { idx, entry ->
            val cx = slotW * idx + slotW / 2f
            val h = chartH * (entry.walkCount.toFloat() / maxCount)
            if (entry.walkCount > 0) {
                drawRoundRect(
                    color = ColorWalk,
                    topLeft = Offset(cx - barW / 2, padTop + chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }

        // Duration overlay line (dashed feel via short segments)
        val durPath = Path()
        var started = false
        visible.forEachIndexed { idx, entry ->
            if (entry.avgDurationMinutes > 0f) {
                val cx = slotW * idx + slotW / 2f
                val y = padTop + chartH * (1f - entry.avgDurationMinutes / maxDur)
                if (!started) { durPath.moveTo(cx, y); started = true } else durPath.lineTo(cx, y)
            }
        }
        if (started) drawPath(durPath, ColorWalk.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
    }

    if (visible.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(visible.first().label, color = OchreColors.TextSecondary, fontSize = 10.sp)
            if (visible.size > 2) Text(visible[visible.size / 2].label, color = OchreColors.TextSecondary, fontSize = 10.sp)
            Text(visible.last().label, color = OchreColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

// ── 24h routine chart ─────────────────────────────────────────────────────────

@Composable
private fun DailyRoutineChart(hourly: HourlyBucket) {
    val maxVal = (0 until 24).maxOf { h ->
        hourly.walkStarts[h] + hourly.pees[h] + hourly.poos[h] + hourly.feeds[h]
    }.coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val padTop = 8f
        val padBottom = 4f
        val chartH = size.height - padTop - padBottom
        val slotW = size.width / 24f

        // Hour grid lines every 6h
        val grid = OchreColors.TextSecondary.copy(alpha = 0.12f)
        for (h in listOf(0, 6, 12, 18, 24)) {
            val x = slotW * h
            drawLine(grid, Offset(x, padTop), Offset(x, padTop + chartH), strokeWidth = 1f)
        }

        for (h in 0 until 24) {
            val x = slotW * h
            val w = slotW * 0.9f
            val offset = (slotW - w) / 2

            // Stacked bars: walk at bottom, then feed, pee, poo on top
            val layers = listOf(
                hourly.walkStarts[h] to ColorWalk,
                hourly.feeds[h] to ColorFeed,
                hourly.pees[h] to ColorPee,
                hourly.poos[h] to ColorPoo
            )
            var stackY = padTop + chartH
            layers.forEach { (count, color) ->
                if (count > 0) {
                    val h2 = chartH * (count.toFloat() / maxVal)
                    stackY -= h2
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x + offset, stackY),
                        size = Size(w, h2),
                        cornerRadius = CornerRadius(1.5f)
                    )
                }
            }
        }
    }
}

// ── Poo-after-food histogram ──────────────────────────────────────────────────

@Composable
private fun PooAfterFoodChart(minutesList: List<Int>) {
    // Bucket into 30-min intervals up to 8h (16 buckets)
    val bucketSize = 30
    val numBuckets = 16  // 0–8h in 30-min slots
    val counts = IntArray(numBuckets)
    minutesList.forEach { m ->
        val b = (m / bucketSize).coerceIn(0, numBuckets - 1)
        counts[b]++
    }
    val maxCount = counts.max().coerceAtLeast(1)
    val grid = OchreColors.TextSecondary.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val padTop = 8f
        val padBottom = 20f
        val chartH = size.height - padTop - padBottom
        val slotW = size.width / numBuckets
        val barW = slotW * 0.75f

        for (i in 0..3) {
            val y = padTop + chartH * (1f - i / 3f)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        for (b in 0 until numBuckets) {
            if (counts[b] > 0) {
                val h = chartH * (counts[b].toFloat() / maxCount)
                val x = slotW * b + (slotW - barW) / 2
                drawRoundRect(
                    color = ColorPoo,
                    topLeft = Offset(x, padTop + chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }
    }

    // X-axis: 0, 2h, 4h, 6h, 8h
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("0", "2h", "4h", "6h", "8h").forEach { label ->
            Text(label, color = OchreColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = OchreColors.TextSecondary,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(modifier = Modifier.size(7.dp)) {
            drawCircle(color)
        }
        Text(label, color = OchreColors.TextSecondary, fontSize = 11.sp)
    }
}
