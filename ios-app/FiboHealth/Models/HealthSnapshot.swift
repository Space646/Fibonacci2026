import Foundation

struct HealthSnapshot: Codable {
    var date: String           // "YYYY-MM-DD"
    var steps: Int
    var caloriesBurned: Double
    var activeMinutes: Int
    var workouts: Int

    enum CodingKeys: String, CodingKey {
        case date
        case steps
        case caloriesBurned  = "calories_burned"
        case activeMinutes   = "active_minutes"
        case workouts
    }

    static var empty: HealthSnapshot {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return HealthSnapshot(date: formatter.string(from: Date()),
                              steps: 0, caloriesBurned: 0, activeMinutes: 0, workouts: 0)
    }
}
