package com.example.autoreboot

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class RebootAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RebootScheduler.action()) return

        AutoRebootState.recordDiagnostic(context, "AlarmReceiver: received")
        AutoRebootState.recordAlarm(context)

        AutoRebootState.recordDiagnostic(context, "AlarmReceiver: beginning validation")

        val expectedEnd = intent.getLongExtra(RebootScheduler.timerEndExtra(), 0L)
        val currentEnd = AutoRebootState.timerEnd(context)
        val timerStart = AutoRebootState.timerStart(context)
        val currentToken = AutoRebootState.timerToken(context)
        val now = System.currentTimeMillis()

        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed device owner check")
            AutoRebootState.recordRebootAttempt(context, "skipped: not device owner")
            return
        }

        if (!AutoRebootState.isEnabled(context)) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed enabled check")
            AutoRebootState.recordRebootAttempt(context, "skipped: disabled")
            return
        }

        if (!keyguard.isDeviceLocked) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed locked check")
            AutoRebootState.recordRebootAttempt(context, "skipped: device unlocked")
            AutoRebootState.clearTimer(context)
            return
        }

        if (currentEnd == 0L || timerStart == 0L || currentToken == 0L) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed timer state check")
            AutoRebootState.recordRebootAttempt(context, "skipped: no active timer")
            return
        }

        if (expectedEnd != currentEnd || expectedEnd != currentToken) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed stale alarm check")
            AutoRebootState.recordRebootAttempt(context, "skipped: stale alarm")
            return
        }

        if (now < currentEnd) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: fired before timer expiry")
            AutoRebootState.recordRebootAttempt(context, "skipped: timer not expired")
            return
        }

        if (AutoRebootState.lastUnlock(context) >= timerStart) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: failed post-lock unlock check")
            AutoRebootState.recordRebootAttempt(
                context,
                "skipped: user unlocked after timer start"
            )
            AutoRebootState.clearTimer(context)
            return
        }

        try {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: invalidating expired timer before reboot")
            RebootScheduler.cancel(context)
            AutoRebootState.clearTimer(context)
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: calling DevicePolicyManager.reboot")
            AutoRebootState.recordRebootAttempt(context, "reboot requested")
            dpm.reboot(ComponentName(context, AutoRebootDeviceAdmin::class.java))
        } catch (e: SecurityException) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: SecurityException: " + (e.message ?: "unknown"))
            AutoRebootState.recordRebootAttempt(
                context,
                "reboot failed: SecurityException: " + (e.message ?: "unknown")
            )
        } catch (e: RuntimeException) {
            AutoRebootState.recordDiagnostic(context, "AlarmReceiver: " + e.javaClass.simpleName + ": " + (e.message ?: "unknown"))
            AutoRebootState.recordRebootAttempt(
                context,
                "reboot failed: " + e.javaClass.simpleName + ": " + (e.message ?: "unknown")
            )
        }
    }
}
