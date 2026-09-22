package com.example.autoreboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_BOOT_COMPLETED
        ) return

        // Only initialize the post-boot state once. BOOT_COMPLETED can arrive
        // after the user has already unlocked on some devices.
        if (!AutoRebootState.isWaitingForFirstUnlock(context)) {
            AutoRebootState.setWaitingForFirstUnlock(context, true)
        }

        AutoRebootState.clearTimer(context)
        RebootScheduler.cancelAll(context)
    }
}
