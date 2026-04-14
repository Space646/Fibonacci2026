import Foundation

struct SessionState: Codable {
    var caloriesConsumed: Double
    var caloriesRemaining: Double
    var lastScanFood: String
    var lastScanKcal: Double

    enum CodingKeys: String, CodingKey {
        case caloriesConsumed  = "calories_consumed"
        case caloriesRemaining = "calories_remaining"
        case lastScanFood      = "last_scan_food"
        case lastScanKcal      = "last_scan_kcal"
    }

    static var empty: SessionState {
        SessionState(caloriesConsumed: 0, caloriesRemaining: 0,
                     lastScanFood: "", lastScanKcal: 0)
    }
}
