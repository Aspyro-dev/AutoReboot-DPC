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
    private const val KEY_LAST_SCREEN_OFF = "last_screen_off"
    private const val KEY_LAST_LOCK_CHECK = "last_lock_check"
    private const val KEY_LAST_LOCK_CHECK_RESULT = "last_lock_check_result"
    private const val KEY_LAST_KEYGUARD_CHECK_RESULT = "last_keyguard_check_result"
    private const val KEY_LAST_KEYGUARD_SECURE_RESULT = "last_keyguard_secure_result"
    private const val KEY_LAST_INTERACTIVE_RESULT = "last_interactive_result"
    private const val KEY_LAST_TIMER_SCHEDULED = "last_timer_scheduled"
    private const val KEY_LAST_TIMER_CLEARED = "last_timer_cleared"
    private const val KEY_LAST_TIMER_CLEAR_REASON = "last_timer_clear_reason"
    private const val KEY_LAST_USER_PRESENT = "last_user_present"
    private const val KEY_LAST_SERVICE_CREATE = "last_service_create"
    private const val KEY_LAST_SERVICE_DESTROY = "last_service_destroy"

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
        prefs(context).edit()
            .putLong(KEY_LAST_LOCK, System.currentTimeMillis())
            .apply()
    }

    fun startTimer(context: Context, endTime: Long) {
        prefs(context).edit()
            .putLong(KEY_TIMER_END, endTime)
            .apply()
    }

    fun clearTimer(context: Context, reason: String = "unknown") {
        prefs(context).edit()
            .remove(KEY_TIMER_END)
            .putLong(KEY_LAST_TIMER_CLEARED, System.currentTimeMillis())
            .putString(KEY_LAST_TIMER_CLEAR_REASON, reason)
            .apply()
    }

    fun setWaitingForFirstUnlock(context: Context, waiting: Boolean) {
        prefs(context).edit().putBoolean(KEY_WAITING_FIRST_UNLOCK, waiting).apply()
    }

    fun isWaitingForFirstUnlock(context: Context) =
        prefs(context).getBoolean(KEY_WAITING_FIRST_UNLOCK, false)

    fun recordUnlock(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_UNLOCK, System.currentTimeMillis())
            .putLong(KEY_LAST_USER_PRESENT, System.currentTimeMillis())
            .apply()
    }

    fun recordAlarm(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_ALARM, System.currentTimeMillis())
            .apply()
    }

    fun recordScreenOff(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_SCREEN_OFF, System.currentTimeMillis())
            .apply()
    }

    fun recordLockCheck(
        context: Context,
        locked: Boolean,
        keyguardLocked: Boolean,
        keyguardSecure: Boolean,
        interactive: Boolean
    ) {
        prefs(context).edit()
            .putLong(KEY_LAST_LOCK_CHECK, System.currentTimeMillis())
            .putString(KEY_LAST_LOCK_CHECK_RESULT, if (locked) "LOCKED" else "UNLOCKED")
            .putString(KEY_LAST_KEYGUARD_CHECK_RESULT, if (keyguardLocked) "LOCKED" else "UNLOCKED")
            .putString(KEY_LAST_KEYGUARD_SECURE_RESULT, if (keyguardSecure) "SECURE" else "NOT SECURE")
            .putString(KEY_LAST_INTERACTIVE_RESULT, if (interactive) "INTERACTIVE" else "NOT INTERACTIVE")
            .apply()
    }

    fun recordTimerScheduled(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_TIMER_SCHEDULED, System.currentTimeMillis())
            .apply()
    }

    fun recordServiceCreate(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_SERVICE_CREATE, System.currentTimeMillis())
            .apply()
    }

    fun recordServiceDestroy(context: Context) {
        prefs(context).edit()
            .putLong(KEY_LAST_SERVICE_DESTROY, System.currentTimeMillis())
            .apply()
    }

    fun lastUnlock(context: Context) = prefs(context).getLong(KEY_LAST_UNLOCK, 0L)
    fun lastLock(context: Context) = prefs(context).getLong(KEY_LAST_LOCK, 0L)
    fun lastAlarm(context: Context) = prefs(context).getLong(KEY_LAST_ALARM, 0L)
    fun lastScreenOff(context: Context) = prefs(context).getLong(KEY_LAST_SCREEN_OFF, 0L)
    fun lastLockCheck(context: Context) = prefs(context).getLong(KEY_LAST_LOCK_CHECK, 0L)
    fun lastLockCheckResult(context: Context) =
        prefs(context).getString(KEY_LAST_LOCK_CHECK_RESULT, "never") ?: "never"
    fun lastKeyguardCheckResult(context: Context) =
        prefs(context).getString(KEY_LAST_KEYGUARD_CHECK_RESULT, "never") ?: "never"
    fun lastKeyguardSecureResult(context: Context) =
        prefs(context).getString(KEY_LAST_KEYGUARD_SECURE_RESULT, "never") ?: "never"
    fun lastInteractiveResult(context: Context) =
        prefs(context).getString(KEY_LAST_INTERACTIVE_RESULT, "never") ?: "never"
    fun lastTimerScheduled(context: Context) =
        prefs(context).getLong(KEY_LAST_TIMER_SCHEDULED, 0L)
    fun lastTimerCleared(context: Context) =
        prefs(context).getLong(KEY_LAST_TIMER_CLEARED, 0L)
    fun lastTimerClearReason(context: Context) =
        prefs(context).getString(KEY_LAST_TIMER_CLEAR_REASON, "never") ?: "never"
    fun lastUserPresent(context: Context) =
        prefs(context).getLong(KEY_LAST_USER_PRESENT, 0L)
    fun lastServiceCreate(context: Context) =
        prefs(context).getLong(KEY_LAST_SERVICE_CREATE, 0L)
    fun lastServiceDestroy(context: Context) =
        prefs(context).getLong(KEY_LAST_SERVICE_DESTROY, 0L)
}