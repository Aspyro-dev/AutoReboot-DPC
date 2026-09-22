package com.example.autoreboot

import android.content.Context

object AutoRebootState {
    private const val PREFS = "auto_reboot_state"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_TIMER_END = "timer_end"
    private const val KEY_WAITING_FIRST_UNLOCK = "waiting_first_unlock"
    private const val KEY_LAST_UNLOCK = "last_unlock"
    private const val KEY_LAST_LOCK = "last_lock"
    private const val KEY_LAST_ALARM = "last_alarm"
    private const val KEY_LAST_SCHEDULED = "last_scheduled"
    private const val KEY_LAST_SCHEDULED_END = "last_scheduled_end"
    private const val KEY_LAST_SCHEDULE_MODE = "last_schedule_mode"
    private const val KEY_LAST_SCHEDULE_ERROR = "last_schedule_error"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun durationMs(context: Context) =
        prefs(context).getLong(KEY_DURATION_MS, 10L * 60L * 60L * 1000L)

    fun setDurationMs(context: Context, durationMs: Long) {
        prefs(context).edit().putLong(KEY_DURATION_MS, durationMs).apply()
    }

    fun timerEnd(context: Context) =
        prefs(context).getLong(KEY_TIMER_END, 0L)

    fun recordLock(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_LOCK, System.currentTimeMillis()).apply()
    }

    fun startTimer(context: Context, endTime: Long) {
        prefs(context).edit().putLong(KEY_TIMER_END, endTime).apply()
    }

    fun recordTimerScheduled(context: Context, endTime: Long, mode: String) {
        prefs(context).edit()
            .putLong(KEY_LAST_SCHEDULED, System.currentTimeMillis())
            .putLong(KEY_LAST_SCHEDULED_END, endTime)
            .putString(KEY_LAST_SCHEDULE_MODE, mode)
            .remove(KEY_LAST_SCHEDULE_ERROR)
            .apply()
    }

    fun recordScheduleError(context: Context, error: String) {
        prefs(context).edit()
            .putLong(KEY_LAST_SCHEDULED, System.currentTimeMillis())
            .putString(KEY_LAST_SCHEDULE_ERROR, error)
            .apply()
    }

    fun clearTimer(context: Context) {
        prefs(context).edit().remove(KEY_TIMER_END).apply()
    }

    fun setWaitingForFirstUnlock(context: Context, waiting: Boolean) {
        prefs(context).edit().putBoolean(KEY_WAITING_FIRST_UNLOCK, waiting).apply()
    }

    fun isWaitingForFirstUnlock(context: Context) =
        prefs(context).getBoolean(KEY_WAITING_FIRST_UNLOCK, false)

    fun recordUnlock(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_UNLOCK, System.currentTimeMillis()).apply()
    }

    fun recordAlarm(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_ALARM, System.currentTimeMillis()).apply()
    }

    fun lastUnlock(context: Context) = prefs(context).getLong(KEY_LAST_UNLOCK, 0L)
    fun lastLock(context: Context) = prefs(context).getLong(KEY_LAST_LOCK, 0L)
    fun lastAlarm(context: Context) = prefs(context).getLong(KEY_LAST_ALARM, 0L)
    fun lastScheduled(context: Context) = prefs(context).getLong(KEY_LAST_SCHEDULED, 0L)
    fun lastScheduledEnd(context: Context) = prefs(context).getLong(KEY_LAST_SCHEDULED_END, 0L)
    fun lastScheduleMode(context: Context) = prefs(context).getString(KEY_LAST_SCHEDULE_MODE, "never") ?: "never"
    fun lastScheduleError(context: Context) = prefs(context).getString(KEY_LAST_SCHEDULE_ERROR, "") ?: ""
}