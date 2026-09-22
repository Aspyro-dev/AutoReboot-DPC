package com.example.autoreboot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

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
        if (!alarmManager.canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endTime,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context))
    }

    fun action(): String = ACTION_TIMER_EXPIRED
}