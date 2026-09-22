package com.example.autoreboot

import android.app.admin.DevicePolicyManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class RebootAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RebootScheduler.action()) return

        AutoRebootState.recordAlarm(context)

        val expectedEnd = intent.getLongExtra(RebootScheduler.timerEndExtra(), 0L)
        val currentEnd = AutoRebootState.timerEnd(context)
        val timerStart = AutoRebootState.timerStart(context)
        val now = System.currentTimeMillis()

        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            AutoRebootState.recordRebootAttempt(context, "skipped: not device owner")
            return
        }

        if (!AutoRebootState.isEnabled(context)) {
            AutoRebootState.recordRebootAttempt(context, "skipped: disabled")
            return
        }

        if (!keyguard.isDeviceLocked) {
            AutoRebootState.recordRebootAttempt(context, "skipped: device unlocked")
            AutoRebootState.clearTimer(context)
            return
        }

        if (currentEnd == 0L || timerStart == 0L) {
            AutoRebootState.recordRebootAttempt(context, "skipped: no active timer")
            return
        }

        if (expectedEnd != currentEnd) {
            AutoRebootState.recordRebootAttempt(context, "skipped: stale alarm")
            return
        }

        if (now < currentEnd) {
            AutoRebootState.recordRebootAttempt(context, "skipped: timer not expired")
            return
        }

        if (AutoRebootState.lastUnlock(context) >= timerStart) {
            AutoRebootState.recordRebootAttempt(context, "skipped: user unlocked after timer start")
            AutoRebootState.clearTimer(context)
            return
        }

        try {
            AutoRebootState.recordRebootAttempt(context, "reboot requested")
            dpm.reboot(ComponentName(context, AutoRebootDeviceAdmin::class.java))
        } catch (e: SecurityException) {
            AutoRebootState.recordRebootAttempt(
                context,
                "reboot failed: SecurityException: " + (e.message ?: "unknown")
            )
        } catch (e: RuntimeException) {
            AutoRebootState.recordRebootAttempt(
                context,
                "reboot failed: " + e.javaClass.simpleName + ": " + (e.message ?: "unknown")
            )
        }
    }
}
