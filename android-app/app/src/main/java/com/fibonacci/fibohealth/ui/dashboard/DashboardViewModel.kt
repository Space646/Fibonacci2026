package com.fibonacci.fibohealth.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.*
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



data class DashboardUiState(
    val profile: UserProfile = UserProfile(),
    val sessionState: SessionState? = null,
    val recentScans: List<FoodLogEntry> = emptyList(),
    val isConnected: Boolean = false,
    val lastSyncTime: Long? = null,
    val todaySnapshot: HealthSnapshot = HealthSnapshot(),
    val caloriesConsumed: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f,
    val sugarG: Float = 0f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val bleClient: BleClient,
    private val hcService: HealthConnectService
) : ViewModel() {

    private val _snapshot = MutableStateFlow(HealthSnapshot())

    private fun fetchSnapshot() {
        viewModelScope.launch {
            if (hcService.hasAllPermissions()) {
                _snapshot.value = runCatching { hcService.fetchToday() }.getOrDefault(HealthSnapshot())
            }
        }
    }

    fun refresh() = fetchSnapshot()

    init {
        fetchSnapshot()
        viewModelScope.launch {
            bleClient.foodLog
                .distinctUntilChanged { old, new -> old.size == new.size }
                .drop(1)
                .collect { fetchSnapshot() }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(profileRepo.profile, bleClient.sessionState, bleClient.foodLog) { p, s, l -> Triple(p, s, l) },
        combine(bleClient.isConnected, bleClient.lastSyncTime) { c, t -> Pair(c, t) },
        _snapshot
    ) { tripleVal, pairVal, snapshot ->
        val (profile, session, log) = tripleVal
        val (connected, syncTime)   = pairVal
        DashboardUiState(
            profile           = profile,
            sessionState      = session,
            recentScans       = log.takeLast(3),
            isConnected       = connected,
            lastSyncTime      = syncTime,
            todaySnapshot     = snapshot,
            caloriesConsumed  = log.sumOf { it.calories.toDouble() }.toFloat(),
            proteinG          = log.sumOf { it.proteinG?.toDouble() ?: 0.0 }.toFloat(),
            fatG              = log.sumOf { it.fatG?.toDouble() ?: 0.0 }.toFloat(),
            sugarG            = log.sumOf { it.sugarG?.toDouble() ?: 0.0 }.toFloat()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
