package com.fibonacci.fibohealth.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun DeviceScreen(vm: DeviceViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
    ) {
        // BT icon
        val iconColor = if (state.isConnected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(iconColor.copy(alpha = if (state.isConnected) 0.1f else 0.06f))
                .border(
                    2.dp,
                    iconColor.copy(alpha = if (state.isConnected) 0.3f else 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (state.isConnected) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = iconColor
            )
        }

        // Name + status
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "AntiDonut Scanner",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            StatusPill(connected = state.isConnected)
        }

        // Action button
        when {
            state.isConnected -> OutlinedButton(
                onClick = { vm.disconnect() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Disconnect Device", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            state.isScanning -> CircularProgressIndicator(color = Indigo, modifier = Modifier.size(32.dp))
            else -> Button(
                onClick = { vm.scan() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo.copy(alpha = 0.15f), contentColor = Indigo),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Connect Device", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusPill(connected: Boolean) {
    val color = if (connected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (connected) 0.1f else 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (connected) 0.3f else 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(
                if (connected) "Connected" else "Disconnected",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
