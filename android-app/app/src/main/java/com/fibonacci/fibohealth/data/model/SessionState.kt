package com.fibonacci.fibohealth.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionState(
    @SerialName("calories_consumed")  val caloriesConsumed: Float = 0f,
    @SerialName("calories_remaining") val caloriesRemaining: Float = 0f,
    @SerialName("last_scan_food")     val lastScanFood: String = "",
    @SerialName("last_scan_kcal")     val lastScanKcal: Float = 0f
)
