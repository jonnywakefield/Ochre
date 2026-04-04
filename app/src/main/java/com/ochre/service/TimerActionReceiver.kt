package com.ochre.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ochre.app.OchreApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WALK_POO = "com.ochre.action.WALK_POO"
        const val ACTION_WALK_PEE = "com.ochre.action.WALK_PEE"
        const val ACTION_WALK_END = "com.ochre.action.WALK_END"
        const val ACTION_ALONE_END = "com.ochre.action.ALONE_END"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as OchreApp).container
        when (intent.action) {
            ACTION_WALK_POO -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let { container.addPooToWalkUseCase(it.id) }
            }
            ACTION_WALK_PEE -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let { container.addPeeToWalkUseCase(it.id) }
            }
            ACTION_WALK_END -> scope.launch {
                val active = container.getActiveWalkUseCase().first()
                active?.let {
                    container.endWalkUseCase(it.id)
                    context.stopService(Intent(context, WalkTimerService::class.java))
                }
            }
            ACTION_ALONE_END -> scope.launch {
                val active = container.getActiveAloneSessionUseCase().first()
                active?.let {
                    container.endAloneUseCase(it.id)
                    context.stopService(Intent(context, AloneTimerService::class.java))
                }
            }
        }
    }
}
