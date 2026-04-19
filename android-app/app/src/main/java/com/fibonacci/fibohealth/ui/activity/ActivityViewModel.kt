package com.fibonacci.fibohealth.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.HealthSnapshot
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val snapshot: HealthSnapshot = HealthSnapshot(),
    val weeklyBurned: List<Int> = emptyList(),
    val hasPermission: Boolean = false,
    val loading: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val hcService: HealthConnectService,
    private val bleClient: BleClient,
    private val profileRepo: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _state.asStateFlow()
    val permissions get() = hcService.allPermissions

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val hasPerm = hcService.hasAllPermissions()
        if (hasPerm) {
            val snapshot      = runCatching { hcService.fetchToday() }.getOrDefault(HealthSnapshot())
            val weeklyBurned  = runCatching { hcService.fetchWeekCaloriesBurned() }.getOrDefault(emptyList())
            _state.value = ActivityUiState(snapshot, weeklyBurned, true, false)
            val deviceId = profileRepo.getOrDefault().deviceId
            bleClient.healthSnapPayload = snapshot.blePayload(deviceId)
            bleClient.resync()
        } else {
            _state.value = ActivityUiState(hasPermission = false, loading = false)
        }
    }
}
