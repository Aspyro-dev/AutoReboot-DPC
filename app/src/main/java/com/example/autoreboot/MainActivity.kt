package com.example.autoreboot

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autoreboot.ui.theme.AutoRebootTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AutoRebootTheme { AutoRebootScreen() } }
    }
}

@androidx.compose.runtime.Composable
fun AutoRebootScreen() {
    val context = LocalContext.current
    val dpm = context.getSystemService(DevicePolicyManager::class.java)
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    val isDeviceOwner = dpm.isDeviceOwnerApp(context.packageName)
    val isLocked = keyguard.isDeviceLocked
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var refresh by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            refresh = !refresh
            delay(1000L)
        }
    }

    val timerEnd = AutoRebootState.timerEnd(context)
    val remaining = if (timerEnd > now) timerEnd - now else 0L
    val timerActive = timerEnd > now

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AutoReboot", style = MaterialTheme.typography.headlineMedium)
        Text(if (isDeviceOwner) "Device Owner: YES" else "Device Owner: NO", Modifier.padding(top = 16.dp))
        Text(if (isLocked) "Device state: LOCKED" else "Device state: UNLOCKED", Modifier.padding(top = 8.dp))
        Text(if (timerActive) "Timer: ${formatDuration(remaining)} remaining" else "Timer: not active", Modifier.padding(top = 8.dp))
        Text("Enabled: ${AutoRebootState.isEnabled(context)}", Modifier.padding(top = 8.dp))
        Text("Waiting for first unlock: ${AutoRebootState.isWaitingForFirstUnlock(context)}", Modifier.padding(top = 8.dp))

        Text("Last unlock: ${formatTimestamp(AutoRebootState.lastUnlock(context))}", Modifier.padding(top = 8.dp))
        Text("Last lock detected: ${formatTimestamp(AutoRebootState.lastLock(context))}", Modifier.padding(top = 8.dp))
        Text("Last timer alarm: ${formatTimestamp(AutoRebootState.lastAlarm(context))}", Modifier.padding(top = 8.dp))

        Text("Last SCREEN_OFF: ${formatTimestamp(AutoRebootState.lastScreenOff(context))}", Modifier.padding(top = 8.dp))
        Text(
            "Last lock check: ${formatTimestamp(AutoRebootState.lastLockCheck(context))} (${AutoRebootState.lastLockCheckResult(context)})",
            Modifier.padding(top = 8.dp)
        )
        Text("Last Keyguard check: ${AutoRebootState.lastKeyguardCheckResult(context)}", Modifier.padding(top = 8.dp))
        Text("Last timer scheduled: ${formatTimestamp(AutoRebootState.lastTimerScheduled(context))}", Modifier.padding(top = 8.dp))
        Text(
            "Last timer cleared: ${formatTimestamp(AutoRebootState.lastTimerCleared(context))} (${AutoRebootState.lastTimerClearReason(context)})",
            Modifier.padding(top = 8.dp)
        )
        Text("Last USER_PRESENT: ${formatTimestamp(AutoRebootState.lastUserPresent(context))}", Modifier.padding(top = 8.dp))
        Text("Service created: ${formatTimestamp(AutoRebootState.lastServiceCreate(context))}", Modifier.padding(top = 8.dp))
        Text("Service destroyed: ${formatTimestamp(AutoRebootState.lastServiceDestroy(context))}", Modifier.padding(top = 8.dp))

        Button(
            onClick = {
                val enabled = !AutoRebootState.isEnabled(context)
                AutoRebootState.setEnabled(context, enabled)
                if (!enabled) {
                    AutoRebootState.clearTimer(context, "disabled")
                    RebootScheduler.cancel(context)
                }
                refresh = !refresh
            },
            Modifier.padding(top = 16.dp)
        ) {
            Text(if (AutoRebootState.isEnabled(context)) "Disable" else "Enable")
        }

        Button(
            onClick = {
                AutoRebootState.clearTimer(context, "manual_cancel")
                RebootScheduler.cancel(context)
                refresh = !refresh
            },
            Modifier.padding(top = 8.dp)
        ) {
            Text("Cancel current timer")
        }

        Button(
            onClick = {
                val endTime = System.currentTimeMillis() + 60_000L
                AutoRebootState.startTimer(context, endTime)
                AutoRebootState.recordTimerScheduled(context)
                RebootScheduler.schedule(context, endTime)
                refresh = !refresh
            },
            Modifier.padding(top = 8.dp)
        ) {
            Text("Start 1-minute test timer")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "never"
    return java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSS",
        java.util.Locale.getDefault()
    ).format(java.util.Date(timestamp))
}