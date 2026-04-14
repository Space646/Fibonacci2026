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
}
