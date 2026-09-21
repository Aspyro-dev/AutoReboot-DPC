package com.example.autoreboot

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.autoreboot.ui.theme.AutoRebootTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoRebootTheme {
                AutoRebootScreen()
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun AutoRebootScreen() {
    val context = LocalContext.current

    val devicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)

    val isDeviceOwner =
        devicePolicyManager.isDeviceOwnerApp(context.packageName)

    val adminComponent = ComponentName(
        context,
        AutoRebootDeviceAdmin::class.java
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AutoReboot",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (isDeviceOwner) {
                "Device Owner: YES"
            } else {
                "Device Owner: NO"
            },
            modifier = Modifier.padding(top = 16.dp)
        )

        Button(
            onClick = {
                devicePolicyManager.reboot(adminComponent)
            },
            enabled = isDeviceOwner,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Reboot device")
        }
    }
}