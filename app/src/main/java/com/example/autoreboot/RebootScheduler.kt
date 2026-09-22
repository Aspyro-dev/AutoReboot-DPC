package com.example.autoreboot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object RebootScheduler {
    private const val REQUEST_CODE = 7001
    private const val ACTION_TIMER_EXPIRED =
        "com.example.autoreboot.ACTION_TIMER_EXPIRED"
    private const val EXTRA_TIMER_END = "timer_end"

    private fun pendingIntent(context: Context, endTime: Long? = null): PendingIntent {
        val intent = Intent(context, RebootAlarmReceiver::class.java)
            .setAction(ACTION_TIMER_EXPIRED)

        if (endTime != null) {
            intent.putExtra(EXTRA_TIMER_END, endTime)
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, endTime: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, endTime)

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
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                endTime,
                operation
            )
            AutoRebootState.recordTimerScheduled(context, endTime, "exact_allow_while_idle")
        } catch (e: SecurityException) {
            AutoRebootState.recordScheduleError(
                context,
                "SecurityException: " + (e.message ?: "unknown")
            )
        } catch (e: RuntimeException) {
            AutoRebootState.recordScheduleError(
                context,
                e.javaClass.simpleName + ": " + (e.message ?: "unknown")
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context))
    }

    fun action(): String = ACTION_TIMER_EXPIRED

    fun timerEndExtra(): String = EXTRA_TIMER_END
}
