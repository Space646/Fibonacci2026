package com.fibonacci.fibohealth.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.*
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val profile: UserProfile = UserProfile(),
    val sessionState: SessionState? = null,
    val recentScans: List<FoodLogEntry> = emptyList(),
    val isConnected: Boolean = false,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val bleClient: BleClient
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepo.profile,
        bleClient.sessionState,
        bleClient.foodLog,
        bleClient.isConnected,
        bleClient.lastSyncTime
    ) { profile, session, log, connected, syncTime ->
        DashboardUiState(profile, session, log.takeLast(3), connected, syncTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
