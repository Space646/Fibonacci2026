import Foundation

struct FoodLogEntry: Codable, Identifiable {
    var id: Int
    var foodName: String
    var weightG: Double
    var calories: Double
    var isHealthy: Bool
    var timestamp: String

    // Per-entry nutrient grams. Optional because older Pi payloads omit them;
    // missing => the corresponding HealthKit sample is not written.
    var proteinG: Double?
    var fatG: Double?
    var sugarG: Double?
    var fiberG: Double?

    enum CodingKeys: String, CodingKey {
        case id
        case foodName    = "food_name"
        case weightG     = "weight_g"
        case calories
        case isHealthy   = "is_healthy"
        case timestamp
        case proteinG    = "protein_g"
        case fatG        = "fat_g"
        case sugarG      = "sugar_g"
        case fiberG      = "fiber_g"
    }

    init(id: Int, foodName: String, weightG: Double, calories: Double,
         isHealthy: Bool, timestamp: String,
         proteinG: Double? = nil, fatG: Double? = nil,
         sugarG: Double? = nil, fiberG: Double? = nil) {
        self.id = id
        self.foodName = foodName
        self.weightG = weightG
        self.calories = calories
        self.isHealthy = isHealthy
        self.timestamp = timestamp
        self.proteinG = proteinG
        self.fatG = fatG
        self.sugarG = sugarG
        self.fiberG = fiberG
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
        timestamp = try c.decodeIfPresent(String.self, forKey: .timestamp) ?? ""

        if let b = try? c.decode(Bool.self, forKey: .isHealthy) {
            isHealthy = b
        } else if let i = try? c.decode(Int.self, forKey: .isHealthy) {
            isHealthy = i != 0
        } else {
            isHealthy = false
        }

        proteinG = try c.decodeIfPresent(Double.self, forKey: .proteinG)
        fatG     = try c.decodeIfPresent(Double.self, forKey: .fatG)
        sugarG   = try c.decodeIfPresent(Double.self, forKey: .sugarG)
        fiberG   = try c.decodeIfPresent(Double.self, forKey: .fiberG)
    }
}
