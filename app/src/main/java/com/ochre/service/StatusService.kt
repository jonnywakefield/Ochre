package com.ochre.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.ochre.R
import com.ochre.app.MainActivity
import com.ochre.app.OchreApp
import com.ochre.domain.model.AloneSession
import com.ochre.domain.model.DogEvent
import com.ochre.domain.model.EventType
import com.ochre.domain.model.MealScheduleEntry
import com.ochre.domain.model.WalkSession
import com.ochre.service.OchreNotificationManager.CHANNEL_STATUS
import com.ochre.service.OchreNotificationManager.NOTIF_ID_STATUS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.Calendar

private data class StatusSnapshot(
    val activeWalk: WalkSession?,
    val activeAlone: AloneSession?,
    val lastEvents: Map<EventType, DogEvent?>,
    val meals: List<MealScheduleEntry>,
    val walkHistory: List<WalkSession>,
    val tick: Unit
)

class StatusService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ticker = flow { while (true) { emit(Unit); delay(10_000) } }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID_STATUS, buildPlaceholder())

        val container = (applicationContext as OchreApp).container

        scope.launch {
            val dataFlow = combine(
                container.getActiveWalkUseCase(),
                container.getActiveAloneSessionUseCase()
            ) { activeWalk, activeAlone ->
                Pair(activeWalk, activeAlone)
            }.combine(
                combine(
                    container.getLastEventPerTypeUseCase(),
                    container.getMealScheduleUseCase()
                ) { lastEvents, meals -> Pair(lastEvents, meals) }
            ) { (activeWalk, activeAlone), (lastEvents, meals) ->
                Triple(activeWalk, activeAlone, Pair(lastEvents, meals))
            }.combine(container.getWalkHistoryUseCase()) { (activeWalk, activeAlone, lm), walkHistory ->
                Quintuple(activeWalk, activeAlone, lm.first, lm.second, walkHistory)
            }

            combine(dataFlow, ticker) { data, _ ->
                StatusSnapshot(data.a, data.b, data.c, data.d, data.e, Unit)
            }.collect { snap ->
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                val now = System.currentTimeMillis()
                nm.notify(NOTIF_ID_STATUS, buildNotification(snap, now))
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    // ─────────────────────────────────────────────────────────────────────────

    private fun buildNotification(snap: StatusSnapshot, now: Long): Notification {
        val lastWalk = snap.walkHistory.filter { !it.isActive }
            .maxByOrNull { it.endMillis ?: it.startMillis }
        val lastFeed = snap.lastEvents[EventType.FEED]

        // ── Title ─────────────────────────────────────────────────────────────
        val title = when {
            snap.activeWalk != null  -> "Walking  ${formatHHMM(now - snap.activeWalk.startMillis)}"
            snap.activeAlone != null -> "Away  ${formatHHMM(now - snap.activeAlone.startMillis)}"
            else                     -> "ochre"
        }

        // ── Suggested feed time — next upcoming meal's randomised time ───────
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val nowMinForFeed = calNow.get(Calendar.HOUR_OF_DAY) * 60 + calNow.get(Calendar.MINUTE)
        val daySeedForFeed = calNow.get(Calendar.YEAR) * 1000L + calNow.get(Calendar.DAY_OF_YEAR)

        val suggestedFeedTime: String? = snap.meals
            .filter { it.randomReminderEnabled }
            .mapNotNull { meal ->
                val rng = java.util.Random(daySeedForFeed + meal.id)
                val offsetMin = if (meal.windowMinutes > 0) rng.nextInt(meal.windowMinutes) else 0
                val totalMin = meal.targetHour * 60 + meal.targetMinute - meal.windowMinutes / 2 + offsetMin
                if (totalMin > nowMinForFeed) totalMin else null
            }
            .minOrNull()
            ?.let { totalMin -> "%02d:%02d".format((totalMin / 60) % 24, totalMin % 60) }

        // ── Summary line ──────────────────────────────────────────────────────
        val walkPart = when {
            snap.activeWalk != null -> "Walk  ${formatHHMM(now - snap.activeWalk.startMillis)}"
            lastWalk != null        -> "Walk  ${formatHHMM(now - (lastWalk.endMillis ?: lastWalk.startMillis))}"
            else                    -> "Walk  —"
        }
        val foodPart = buildString {
            append(lastFeed?.let { "Food  ${formatHHMM(now - it.timestampMillis)}" } ?: "Food  —")
            if (suggestedFeedTime != null) append("  ($suggestedFeedTime)")
        }
        val awayPart = snap.activeAlone?.let { "Away  ${formatHHMM(now - it.startMillis)}" }
        val summaryLine = listOfNotNull(walkPart, foodPart, awayPart).joinToString("  ·  ")

        // ── Progress bar ──────────────────────────────────────────────────────
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayStartMs = now - nowMin * 60_000L

        val prefs = NotificationPrefs.get(this)
        val barStartMin = prefs.barStartHour * 60
        val barEndMin   = prefs.barEndHour * 60
        val barSpan     = (barEndMin - barStartMin).coerceAtLeast(60)

        fun barPos(absMin: Int) = (absMin - barStartMin).coerceIn(0, barSpan)

        val nowPos = barPos(nowMin)

        val dog = IconCompat.createWithResource(this, R.drawable.ic_dog_run)

        val style = NotificationCompat.ProgressStyle()
            .setProgress(nowPos)
            .setProgressTrackerIcon(dog)
            .setProgressStartIcon(hourLabelIcon(prefs.barStartHour))
            .setProgressEndIcon(hourLabelIcon(prefs.barEndHour))

        // Collect all painted regions as (start, end, color), sorted by start.
        // Priority (higher index = drawn later / wins): meal window < feed < walk < active-walk
        data class Region(val start: Int, val end: Int, val color: Int)
        val regions = mutableListOf<Region>()

        // Meal windows — subtle dark background hint
        snap.meals.forEach { meal ->
            val center = meal.targetHour * 60 + meal.targetMinute
            val half   = meal.windowMinutes / 2
            val s = barPos(center - half)
            val e = barPos(center + half)
            if (e > s) regions += Region(s, e, 0xFF3A2010.toInt())
        }

        // Past feed events today — 2-minute wide ochre mark
        snap.lastEvents[EventType.FEED]?.let { feed ->
            if (feed.timestampMillis in dayStartMs..now) {
                val absMin = ((feed.timestampMillis - dayStartMs) / 60_000).toInt()
                val s = barPos(absMin)
                val e = barPos(absMin + 2).coerceAtMost(barSpan)
                if (e > s) regions += Region(s, e, 0xFFE4A853.toInt()) // ochre
            }
        }

        // Past walks today — span their actual duration in a warm brown
        snap.walkHistory.filter { !it.isActive }.forEach { walk ->
            val endMs   = walk.endMillis ?: return@forEach
            if (endMs < dayStartMs || walk.startMillis > now) return@forEach
            val startMin = ((walk.startMillis - dayStartMs) / 60_000).toInt()
            val endMin   = ((endMs           - dayStartMs) / 60_000).toInt()
            val s = barPos(startMin)
            val e = barPos(endMin).coerceAtMost(barSpan)
            if (e > s) regions += Region(s, e, 0xFFA0724A.toInt()) // warm brown
        }

        // Active walk — from start to now, brighter colour
        snap.activeWalk?.let { walk ->
            val startMin = ((walk.startMillis - dayStartMs) / 60_000).toInt()
            val s = barPos(startMin)
            val e = nowPos
            if (e > s) regions += Region(s, e, 0xFFD4925A.toInt()) // lighter brown
        }

        // Flatten regions onto a segment list, grey fills the gaps.
        // Merge overlapping regions (last one in list wins for a given position).
        val colorAt = IntArray(barSpan) { 0xFF2A2A2A.toInt() }
        regions.sortedBy { it.start }.forEach { r ->
            for (i in r.start until r.end) colorAt[i] = r.color
        }

        // RLE-encode into segments
        if (barSpan > 0) {
            var segColor = colorAt[0]
            var segLen   = 1
            for (i in 1 until barSpan) {
                if (colorAt[i] == segColor) {
                    segLen++
                } else {
                    style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(segLen).setColor(segColor))
                    segColor = colorAt[i]
                    segLen   = 1
                }
            }
            style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(segLen).setColor(segColor))
        }

        val icon = when {
            snap.activeWalk != null  -> R.drawable.ic_dog_walking
            snap.activeAlone != null -> R.drawable.ic_dog_alone
            lastFeed != null && now - lastFeed.timestampMillis < 30 * 60_000 -> R.drawable.ic_dog_eating
            else                     -> R.drawable.ic_dog
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setRequestPromotedOngoing(true)
            .setStyle(style)

        if (snap.activeWalk != null) {
            builder
                .addAction(NotificationCompat.Action(0, "Poo", walkPooIntent()))
                .addAction(NotificationCompat.Action(0, "Pee", walkPeeIntent()))
                .addAction(NotificationCompat.Action(0, "End walk", walkEndIntent()))
        } else if (snap.activeAlone != null) {
            builder.addAction(NotificationCompat.Action(0, "Back home", aloneEndIntent()))
        }

        return builder.build()
    }

    private fun buildPlaceholder(): Notification =
        NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_dog)
            .setContentTitle("ochre")
            .setContentText("Starting…")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun walkPooIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 10,
        Intent(this, TimerActionReceiver::class.java).apply { action = TimerActionReceiver.ACTION_WALK_POO },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun walkPeeIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 11,
        Intent(this, TimerActionReceiver::class.java).apply { action = TimerActionReceiver.ACTION_WALK_PEE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun walkEndIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 12,
        Intent(this, TimerActionReceiver::class.java).apply { action = TimerActionReceiver.ACTION_WALK_END },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun aloneEndIntent(): PendingIntent = PendingIntent.getBroadcast(
        this, 13,
        Intent(this, TimerActionReceiver::class.java).apply { action = TimerActionReceiver.ACTION_ALONE_END },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Renders an hour number (e.g. "9", "23") as a tiny white bitmap for use as a progress end-cap icon. */
    private fun hourLabelIcon(hour: Int): IconCompat {
        val label = hour.toString()
        val sizePx = 20
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val yPos = (sizePx / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(label, sizePx / 2f, yPos, paint)
        return IconCompat.createWithBitmap(bmp)
    }

    // HH:MM — hours and minutes (not minutes and seconds)
    private fun formatHHMM(millis: Long): String {
        val totalMin = millis / 60_000
        val h = totalMin / 60
        val m = totalMin % 60
        return "%d:%02d".format(h, m)
    }
}
