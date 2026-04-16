package com.fibonacci.fibohealth.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DeviceUiState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class DeviceViewModel @Inject constructor(private val bleClient: BleClient) : ViewModel() {

    val uiState: StateFlow<DeviceUiState> = combine(
        bleClient.isConnected, bleClient.isScanning, bleClient.lastSyncTime
    ) { connected, scanning, sync -> DeviceUiState(connected, scanning, sync) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceUiState())

    fun scan() = bleClient.startScan()
    fun disconnect() = bleClient.disconnect()
}
