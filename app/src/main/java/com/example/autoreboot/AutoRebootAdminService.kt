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
            AutoRebootState.recordDiagnostic(context, "AdminService broadcast: " + (intent.action ?: "null"))
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> handler.postDelayed({ handlePossibleLock() }, 500L)
                Intent.ACTION_USER_PRESENT -> handleUnlock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AutoRebootState.recordDiagnostic(this, "AdminService onCreate")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        registerReceiver(lockReceiver, filter)
    }

    override fun onDestroy() {
        AutoRebootState.recordDiagnostic(this, "AdminService onDestroy")
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(lockReceiver)
        super.onDestroy()
    }

    private fun handlePossibleLock() {
        AutoRebootState.recordDiagnostic(this, "handlePossibleLock")
        val keyguard = getSystemService(KeyguardManager::class.java)
        val deviceLocked = keyguard.isDeviceLocked

        if (!AutoRebootState.isEnabled(this)) return
        if (!deviceLocked) return
        if (AutoRebootState.isWaitingForFirstUnlock(this)) return
        if (AutoRebootState.timerEnd(this) > 0L) return

        AutoRebootState.recordLock(this)

        val endTime = System.currentTimeMillis() + AutoRebootState.durationMs(this)
        AutoRebootState.startTimer(this, endTime)
        RebootScheduler.schedule(this, endTime)
    }

    private fun handleUnlock() {
        AutoRebootState.recordDiagnostic(this, "handleUnlock")
        AutoRebootState.recordUnlock(this)

        if (AutoRebootState.isWaitingForFirstUnlock(this)) {
            AutoRebootState.setWaitingForFirstUnlock(this, false)
            RebootScheduler.cancel(this)
            AutoRebootState.clearTimer(this)
            return
        }

        RebootScheduler.cancel(this)
        AutoRebootState.clearTimer(this)
    }
}
