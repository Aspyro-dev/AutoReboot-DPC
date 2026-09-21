package com.example.autoreboot

import android.app.KeyguardManager
import android.app.admin.DeviceAdminService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

class AutoRebootAdminService : DeviceAdminService() {

    private val handler = Handler(Looper.getMainLooper())

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    AutoRebootState.recordScreenOff(context)
                    handler.postDelayed({ handlePossibleLock() }, 500L)
                }

                Intent.ACTION_SCREEN_ON -> {
                    handleScreenOn()
                }

                Intent.ACTION_USER_PRESENT -> {
                    handleUnlock()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AutoRebootState.recordServiceCreate(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        registerReceiver(lockReceiver, filter)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(lockReceiver)
        AutoRebootState.recordServiceDestroy(this)
        super.onDestroy()
    }

    private fun handleScreenOn() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val power = getSystemService(PowerManager::class.java)
        AutoRebootState.recordScreenOn(
            this,
            keyguard.isDeviceLocked,
            keyguard.isKeyguardLocked,
            keyguard.isKeyguardSecure,
            power.isInteractive
        )
    }

    private fun handlePossibleLock() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val power = getSystemService(PowerManager::class.java)
        val deviceLocked = keyguard.isDeviceLocked
        val keyguardLocked = keyguard.isKeyguardLocked
        val keyguardSecure = keyguard.isKeyguardSecure
        val interactive = power.isInteractive

        AutoRebootState.recordLockCheck(
            this,
            deviceLocked,
            keyguardLocked,
            keyguardSecure,
            interactive
        )

        if (!AutoRebootState.isEnabled(this)) return
        if (!deviceLocked) return
        if (AutoRebootState.isWaitingForFirstUnlock(this)) return
        if (AutoRebootState.timerEnd(this) > 0L) return

        AutoRebootState.recordLock(this)

        val endTime = System.currentTimeMillis() + AutoRebootState.durationMs(this)
        AutoRebootState.startTimer(this, endTime)
        AutoRebootState.recordTimerScheduled(this)
        RebootScheduler.schedule(this, endTime)
    }

    private fun handleUnlock() {
        AutoRebootState.recordUnlock(this)

        if (AutoRebootState.isWaitingForFirstUnlock(this)) {
            AutoRebootState.setWaitingForFirstUnlock(this, false)
            AutoRebootState.clearTimer(this, "first_unlock_after_boot")
            RebootScheduler.cancel(this)
            return
        }

        AutoRebootState.clearTimer(this, "user_present")
        RebootScheduler.cancel(this)
    }
}