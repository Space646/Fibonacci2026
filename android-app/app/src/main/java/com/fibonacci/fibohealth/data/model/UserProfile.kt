package com.fibonacci.fibohealth.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

@Entity(tableName = "user_profile")
@Serializable
data class UserProfile(
    @PrimaryKey val deviceId: String = "",
    val name: String = "",
    val age: Int = 25,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val sex: String = "male",       // "male" | "female"
    val activityLevel: String = "moderate",
    val dailyCalorieGoal: Int? = null
) {
    val calculatedDailyGoal: Int get() {
        val bmr = if (sex == "male")
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) + 5
        else
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) - 161
        val multiplier = when (activityLevel) {
            "sedentary" -> 1.2f
            "light"     -> 1.375f
            "moderate"  -> 1.55f
            "active"    -> 1.725f
            else        -> 1.55f
        }
        return (bmr * multiplier).roundToInt()
    }

    fun blePayload(): ByteArray = buildJsonObject {
        put("device_id", deviceId)
        put("name", name)
        put("age", age)
        put("weight_kg", weightKg)
        put("height_cm", heightCm)
        put("sex", sex)
        put("activity_level", activityLevel)
        put("daily_calorie_goal", dailyCalorieGoal?.let { JsonPrimitive(it) } ?: JsonNull)
    }.toString().toByteArray(Charsets.UTF_8)
}
