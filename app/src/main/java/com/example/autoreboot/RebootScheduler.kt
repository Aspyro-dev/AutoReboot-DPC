package com.example.autoreboot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object RebootScheduler {
    private const val REQUEST_CODE_BASE = 7001
    private const val ACTION_TIMER_EXPIRED =
        "com.example.autoreboot.ACTION_TIMER_EXPIRED"
    private const val EXTRA_TIMER_END = "timer_end"

    private fun requestCode(endTime: Long): Int =
        REQUEST_CODE_BASE + (endTime and 0x7FFF).toInt()

    private fun pendingIntent(context: Context, endTime: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(endTime),
            Intent(context, RebootAlarmReceiver::class.java)
                .setAction(ACTION_TIMER_EXPIRED)
                .putExtra(EXTRA_TIMER_END, endTime),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context, endTime: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, endTime)
        AutoRebootState.recordDiagnostic(
            context,
            "RebootScheduler: schedule requested end=" + endTime +
                " now=" + System.currentTimeMillis() +
                " delayMs=" + (endTime - System.currentTimeMillis())
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endTime,
                    operation
                )
                AutoRebootState.recordTimerScheduled(context, endTime, "inexact")
                AutoRebootState.recordDiagnostic(context, "RebootScheduler: scheduled mode=inexact end=" + endTime)
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTime,
                operation
            )
            AutoRebootState.recordTimerScheduled(context, endTime, "exact_allow_while_idle")
            AutoRebootState.recordDiagnostic(context, "RebootScheduler: scheduled mode=exact_allow_while_idle end=" + endTime)
        } catch (e: SecurityException) {
            AutoRebootState.recordScheduleError(
                context,
                "SecurityException: " + (e.message ?: "unknown")
            )
            AutoRebootState.recordDiagnostic(
                context,
                "RebootScheduler: schedule failed SecurityException: " + (e.message ?: "unknown")
            )
        } catch (e: RuntimeException) {
            AutoRebootState.recordScheduleError(
                context,
                e.javaClass.simpleName + ": " + (e.message ?: "unknown")
            )
            AutoRebootState.recordDiagnostic(
                context,
                "RebootScheduler: schedule failed " + e.javaClass.simpleName + ": " + (e.message ?: "unknown")
            )
        }
    }

    fun cancel(context: Context) {
        val token = AutoRebootState.timerToken(context)
        if (token != 0L) {
            context.getSystemService(AlarmManager::class.java)
                .cancel(pendingIntent(context, token))
        }
    }

    fun cancelAll(context: Context) {
        // The current timer, if any, is the only alarm we own.
        cancel(context)
    }

    fun action(): String = ACTION_TIMER_EXPIRED

    fun timerEndExtra(): String = EXTRA_TIMER_END
}
