package com.example.autoreboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                AutoRebootState.recordDiagnostic(
                    context,
                    "BootReceiver: " + intent.action
                )

                // Clear any timer left over from before the reboot. Cancel first
                // so RebootScheduler can still read the timer token.
                RebootScheduler.cancelAll(context)
                AutoRebootState.clearTimer(context)

                // The device must be unlocked once after every reboot before
                // normal lock timers are allowed to start.
                AutoRebootState.setWaitingForFirstUnlock(context, true)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                AutoRebootState.recordDiagnostic(
                    context,
                    "BootReceiver: " + intent.action
                )

                // Do not clear the timer here. On some devices BOOT_COMPLETED
                // can arrive after the user has already unlocked and locked
                // again, in which case clearing it would cancel a newly
                // scheduled post-boot timer.
            }
        }
    }
}
