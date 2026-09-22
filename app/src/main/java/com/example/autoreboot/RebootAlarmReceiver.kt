package com.example.autoreboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RebootAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RebootScheduler.action()) return

        AutoRebootState.recordAlarm(context)

        // Stage 1 intentionally does NOT reboot.
        // We only mark the timer as expired so the behavior can be tested safely.
        AutoRebootState.clearTimer(context)
    }
}
