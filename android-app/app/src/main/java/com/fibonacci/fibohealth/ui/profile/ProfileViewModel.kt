package com.fibonacci.fibohealth.ui.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.UserProfile
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import com.fibonacci.fibohealth.service.HealthConnectFoodLogger
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val HC_LOGGING_KEY = booleanPreferencesKey("hc_food_logging_enabled")

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val bleClient: BleClient,
    private val hcService: HealthConnectService,
    private val hcLogger: HealthConnectFoodLogger,
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) : ViewModel() {

    val profile: StateFlow<UserProfile> =
        profileRepo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val hcLoggingEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[HC_LOGGING_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun save(profile: UserProfile) = viewModelScope.launch {
        profileRepo.save(profile)
        bleClient.profilePayload = profile.blePayload()
    }

    fun setHcLogging(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[HC_LOGGING_KEY] = enabled }
    }

    fun removeAllHcEntries() = viewModelScope.launch {
        hcLogger.removeAll()
    }
}
