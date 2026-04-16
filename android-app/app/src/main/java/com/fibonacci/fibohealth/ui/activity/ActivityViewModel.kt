package com.fibonacci.fibohealth.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.HealthSnapshot
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val snapshot: HealthSnapshot = HealthSnapshot(),
    val hasPermission: Boolean = false,
    val loading: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val hcService: HealthConnectService
) : ViewModel() {

    private val _state = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _state.asStateFlow()
    val permissions get() = hcService.allPermissions   // passed to permission launcher

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val hasPerm = hcService.hasAllPermissions()
        if (hasPerm) {
            val snapshot = runCatching { hcService.fetchToday() }.getOrDefault(HealthSnapshot())
            _state.value = ActivityUiState(snapshot, true, false)
        } else {
            _state.value = ActivityUiState(hasPermission = false, loading = false)
        }
    }
}
