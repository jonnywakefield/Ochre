package com.ochre.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ochre.app.OchreApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules all active alarms after device reboot.
 * AlarmManager alarms are lost on restart — this restores them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { AlarmHelper.scheduleAll(context) }
            finally { pendingResult.finish() }
        }
    }
}
