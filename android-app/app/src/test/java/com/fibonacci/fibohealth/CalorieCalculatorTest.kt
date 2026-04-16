package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {
    @Test fun `male moderate activity TDEE`() {
        val profile = UserProfile(
            deviceId = "test", name = "Test", age = 30,
            weightKg = 80f, heightCm = 180f, sex = "male",
            activityLevel = "moderate", dailyCalorieGoal = null
        )
        // Mifflin-St Jeor: (10×80)+(6.25×180)−(5×30)+5 = 1780 BMR → ×1.55 = 2759
        assertEquals(2759, profile.calculatedDailyGoal)
    }
}
