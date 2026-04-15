import Foundation

struct FoodLogEntry: Codable, Identifiable {
    var id: Int
    var foodName: String
    var weightG: Double
    var calories: Double
    var isHealthy: Bool
    var healthScore: Double
    var timestamp: String

    enum CodingKeys: String, CodingKey {
        case id
        case foodName    = "food_name"
        case weightG     = "weight_g"
        case calories
        case isHealthy   = "is_healthy"
        case healthScore = "health_score"
        case timestamp
    }

    init(id: Int, foodName: String, weightG: Double, calories: Double,
         isHealthy: Bool, healthScore: Double, timestamp: String) {
        self.id = id
        self.foodName = foodName
        self.weightG = weightG
        self.calories = calories
        self.isHealthy = isHealthy
        self.healthScore = healthScore
        self.timestamp = timestamp
    }

    // SQLite on the Pi stores `is_healthy` as INTEGER (0/1), which Python
    // serializes as a JSON number. Swift's default `Bool` decode rejects
    // numeric 0/1, so decode permissively: accept Bool, Int, or numeric string.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        foodName = try c.decode(String.self, forKey: .foodName)
        weightG = try c.decode(Double.self, forKey: .weightG)
        calories = try c.decode(Double.self, forKey: .calories)
        healthScore = try c.decodeIfPresent(Double.self, forKey: .healthScore) ?? 0
        timestamp = try c.decodeIfPresent(String.self, forKey: .timestamp) ?? ""

        if let b = try? c.decode(Bool.self, forKey: .isHealthy) {
            isHealthy = b
        } else if let i = try? c.decode(Int.self, forKey: .isHealthy) {
            isHealthy = i != 0
        } else {
            isHealthy = false
        }
    }
}
