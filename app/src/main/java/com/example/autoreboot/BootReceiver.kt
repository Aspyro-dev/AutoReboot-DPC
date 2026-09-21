package com.example.autoreboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED -> {
                AutoRebootState.setWaitingForFirstUnlock(context, true)
                AutoRebootState.clearTimer(context)
                RebootScheduler.cancel(context)
            }
        }
    }
}
