package com.fibonacci.fibohealth.data.model
import kotlinx.serialization.Serializable

@Serializable
data class HealthSnapshot(
    val date: String = "",
    val steps: Int = 0,
    val caloriesBurned: Float = 0f,
    val activeMinutes: Int = 0,
    val workouts: Int = 0
) {
    fun blePayload(deviceId: String): ByteArray =
        kotlinx.serialization.json.buildJsonObject {
            put("device_id",      kotlinx.serialization.json.JsonPrimitive(deviceId))
            put("date",           kotlinx.serialization.json.JsonPrimitive(date))
            put("steps",          kotlinx.serialization.json.JsonPrimitive(steps))
            put("calories_burned",kotlinx.serialization.json.JsonPrimitive(caloriesBurned))
            put("active_minutes", kotlinx.serialization.json.JsonPrimitive(activeMinutes))
            put("workouts",       kotlinx.serialization.json.JsonPrimitive(workouts))
        }.toString().toByteArray(Charsets.UTF_8)
}
