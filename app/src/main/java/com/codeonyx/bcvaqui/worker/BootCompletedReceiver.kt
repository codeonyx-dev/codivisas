package com.codeonyx.bcvaqui.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules background rate refresh after device reboot. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        WorkScheduler.scheduleAllBackgroundWork(context.applicationContext)
        WorkScheduler.enqueueImmediateRefresh(context.applicationContext)
    }
}
