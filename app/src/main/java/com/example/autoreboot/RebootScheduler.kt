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

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, RebootAlarmReceiver::class.java).setAction(ACTION_TIMER_EXPIRED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context, endTime: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endTime,
                    operation
                )
                AutoRebootState.recordTimerScheduled(context, endTime, "inexact")
                return
            }

            // Use an alarm-clock alarm during this testing stage.
            // Android treats it as an exact, highly visible alarm and does not adjust
            // its delivery time. This gives us a strong test of alarm delivery while
            // the phone is asleep.
            val alarmClockInfo = AlarmManager.AlarmClockInfo(endTime, operation)
            alarmManager.setAlarmClock(alarmClockInfo, operation)
            AutoRebootState.recordTimerScheduled(context, endTime, "alarm_clock_exact")
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
}