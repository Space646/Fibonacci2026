package com.fibonacci.fibohealth.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            "sedentary"   -> 1.2f
            "light"       -> 1.375f
            "moderate"    -> 1.55f
            "active"      -> 1.725f
            "very_active" -> 1.9f
            else          -> 1.55f
        }
        return (bmr * multiplier).roundToInt()
    }

    fun blePayload(): ByteArray = Json.encodeToString(
        mapOf(
            "device_id" to deviceId, "name" to name,
            "age" to age.toString(), "weight_kg" to weightKg.toString(),
            "height_cm" to heightCm.toString(), "sex" to sex,
            "activity_level" to activityLevel,
            "daily_calorie_goal" to (dailyCalorieGoal?.toString() ?: "null")
        )
    ).toByteArray(Charsets.UTF_8)
}
