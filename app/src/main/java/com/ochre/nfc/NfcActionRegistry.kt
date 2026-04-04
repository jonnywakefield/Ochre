package com.ochre.nfc

import android.content.Context
import android.content.Intent
import com.ochre.app.OchreApp
import com.ochre.domain.model.EventType
import com.ochre.service.AloneTimerService
import com.ochre.service.WalkTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Central registry mapping NFC tag payload strings to app actions.
 *
 * Each tag stores an NDEF text record whose payload is one of the TAG_* constants.
 * To support a new tag, write the desired TAG_* value to the tag (use any NDEF
 * writer app, e.g. NFC Tools) and add a handler below if the action isn't already
 * covered.
 *
 * Adding a new action in future = add one `TAG_*` constant + one `register()` call
 * in `registerAll()`. Nothing else changes.
 */
object NfcActionRegistry {

    // ── Tag payload constants — write these strings onto physical NFC tags ──────
    const val TAG_START_WALK   = "ochre:start_walk"
    const val TAG_END_WALK     = "ochre:end_walk"
    const val TAG_LOG_POO      = "ochre:log_poo"
    const val TAG_LOG_PEE      = "ochre:log_pee"
    const val TAG_START_ALONE  = "ochre:start_alone"
    const val TAG_END_ALONE    = "ochre:end_alone"
    const val TAG_LOG_FEED     = "ochre:log_feed"
    const val TAG_LOG_NOTE     = "ochre:log_note"      // opens app to note screen
    const val TAG_LOG_WEIGHT   = "ochre:log_weight"    // opens app to weight entry

    // ── Internal registry ────────────────────────────────────────────────────────
    private val handlers = mutableMapOf<String, suspend (Context) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Registers all known tag → action mappings.
     * Called once from NfcDispatchActivity before dispatch.
     */
    fun registerAll(context: Context) {
        val container = (context.applicationContext as OchreApp).container

        register(TAG_START_WALK) { ctx ->
            val active = container.getActiveWalkUseCase().first()
            if (active == null) {
                container.startWalkUseCase()
                ctx.startForegroundService(Intent(ctx, WalkTimerService::class.java))
            }
            // If walk already active, tap is a no-op (idempotent)
        }

        register(TAG_END_WALK) { ctx ->
            val active = container.getActiveWalkUseCase().first()
            active?.let {
                container.endWalkUseCase(it.id)
                ctx.stopService(Intent(ctx, WalkTimerService::class.java))
            }
        }

        register(TAG_LOG_POO) {
            val active = container.getActiveWalkUseCase().first()
            if (active != null) {
                container.addPooToWalkUseCase(active.id)
            } else {
                container.logEventUseCase(type = EventType.POO)
            }
        }

        register(TAG_LOG_PEE) {
            val active = container.getActiveWalkUseCase().first()
            if (active != null) {
                container.addPeeToWalkUseCase(active.id)
            } else {
                container.logEventUseCase(type = EventType.PEE)
            }
        }

        register(TAG_START_ALONE) { ctx ->
            val active = container.getActiveAloneSessionUseCase().first()
            if (active == null) {
                container.startAloneUseCase()
                ctx.startForegroundService(Intent(ctx, AloneTimerService::class.java))
            }
        }

        register(TAG_END_ALONE) { ctx ->
            val active = container.getActiveAloneSessionUseCase().first()
            active?.let {
                container.endAloneUseCase(it.id)
                ctx.stopService(Intent(ctx, AloneTimerService::class.java))
            }
        }

        register(TAG_LOG_FEED) {
            // Use the default grams from the next scheduled meal, or 0 if none configured
            val meals = container.getMealScheduleUseCase().first()
            val grams = if (meals.isNotEmpty()) meals.first().defaultGrams else 0
            container.logFeedUseCase(grams = grams)
        }

        // Note and Weight open the app — handled by NfcDispatchActivity directly
        // (they need UI input), so no silent handler needed here
    }

    private fun register(tag: String, action: suspend (Context) -> Unit) {
        handlers[tag] = action
    }

    /**
     * Dispatches the action for [tagPayload] if one is registered.
     * Returns true if a handler was found and fired, false otherwise.
     */
    fun dispatch(context: Context, tagPayload: String): Boolean {
        val handler = handlers[tagPayload] ?: return false
        scope.launch { handler(context) }
        return true
    }

    /**
     * Returns true if this payload should open the app rather than
     * run silently in the background.
     */
    fun requiresAppOpen(tagPayload: String): Boolean =
        tagPayload == TAG_LOG_NOTE || tagPayload == TAG_LOG_WEIGHT
}
