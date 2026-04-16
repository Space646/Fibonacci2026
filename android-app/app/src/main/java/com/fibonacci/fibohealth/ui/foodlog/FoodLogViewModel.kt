package com.fibonacci.fibohealth.ui.foodlog

import androidx.lifecycle.ViewModel
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class FoodLogViewModel @Inject constructor(bleClient: BleClient) : ViewModel() {
    val foodLog: StateFlow<List<FoodLogEntry>> = bleClient.foodLog
}
