import SwiftData
import Foundation

@Model
final class UserProfile {
    var id: UUID
    var deviceId: UUID
    var name: String
    var age: Int
    var weightKg: Double
    var heightCm: Double
    var sex: String          // "male" | "female" | "other"
    var activityLevel: String // "sedentary" | "light" | "moderate" | "active"
    var dailyCalorieGoal: Double?  // nil = use calculated BMR
    var createdAt: Date

    init(name: String = "", age: Int = 25, weightKg: Double = 70,
         heightCm: Double = 170, sex: String = "other",
         activityLevel: String = "moderate", dailyCalorieGoal: Double? = nil) {
        self.id = UUID()
        self.deviceId = UUID()
        self.name = name
        self.age = age
        self.weightKg = weightKg
        self.heightCm = heightCm
        self.sex = sex
        self.activityLevel = activityLevel
        self.dailyCalorieGoal = dailyCalorieGoal
        self.createdAt = Date()
    }

    /// JSON payload sent to Pi over BLE
    func blePayload() -> [String: Any] {
        [
            "device_id": deviceId.uuidString,
            "name": name,
            "age": age,
            "weight_kg": weightKg,
            "height_cm": heightCm,
            "sex": sex,
            "activity_level": activityLevel,
            "daily_calorie_goal": dailyCalorieGoal as Any
        ]
    }

    /// Calculated daily calorie goal using Mifflin-St Jeor BMR
    var calculatedDailyGoal: Double {
        if let manual = dailyCalorieGoal { return manual }
        let base = 10 * weightKg + 6.25 * heightCm - 5 * Double(age)
        let bmr: Double
        switch sex {
        case "male":   bmr = base + 5
        case "female": bmr = base - 161
        default:       bmr = base - 78
        }
        let multipliers = ["sedentary": 1.2, "light": 1.375, "moderate": 1.55, "active": 1.725]
        return (bmr * (multipliers[activityLevel] ?? 1.2)).rounded()
    }
}
