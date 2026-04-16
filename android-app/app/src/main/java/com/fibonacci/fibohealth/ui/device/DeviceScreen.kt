package com.fibonacci.fibohealth.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceScreen(vm: DeviceViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Icon(
            if (state.isConnected) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (state.isConnected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (state.isConnected) "Connected to FiboHealth Pi"
            else if (state.isScanning) "Searching\u2026"
            else "Not Connected",
            style = MaterialTheme.typography.titleLarge
        )
        state.lastSyncTime?.let {
            Text(
                "Last sync: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            state.isConnected -> OutlinedButton({ vm.disconnect() }) { Text("Disconnect") }
            state.isScanning  -> CircularProgressIndicator()
            else              -> Button({ vm.scan() }) { Text("Scan for Pi") }
        }
    }
}
