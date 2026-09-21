package com.example.autoreboot

import android.app.KeyguardManager
import android.app.admin.DeviceAdminService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper

class AutoRebootAdminService : DeviceAdminService() {

    private val handler = Handler(Looper.getMainLooper())

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Give Keyguard a moment to settle before checking the
                    // actual device-locked state.
                    handler.postDelayed({ handlePossibleLock() }, 500L)
                }

                Intent.ACTION_USER_PRESENT -> {
                    handleUnlock()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        registerReceiver(lockReceiver, filter)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(lockReceiver)
        super.onDestroy()
    }

    private fun handlePossibleLock() {
        val keyguard = getSystemService(KeyguardManager::class.java)

        if (!AutoRebootState.isEnabled(this)) return
        if (!keyguard.isDeviceLocked) return
        if (AutoRebootState.isWaitingForFirstUnlock(this)) return
        if (AutoRebootState.timerEnd(this) > 0L) return

        AutoRebootState.recordLock(this)

        val endTime = System.currentTimeMillis() + AutoRebootState.durationMs(this)
        AutoRebootState.startTimer(this, endTime)
        RebootScheduler.schedule(this, endTime)
    }

    private fun handleUnlock() {
        AutoRebootState.recordUnlock(this)

        if (AutoRebootState.isWaitingForFirstUnlock(this)) {
            // After boot, the first successful unlock only arms the normal
            // lock/unlock cycle. The timer starts on the next lock.
            AutoRebootState.setWaitingForFirstUnlock(this, false)
            AutoRebootState.clearTimer(this)
            RebootScheduler.cancel(this)
            return
        }

        // Every successful unlock starts a fresh cycle.
        AutoRebootState.clearTimer(this)
        RebootScheduler.cancel(this)
    }
}
