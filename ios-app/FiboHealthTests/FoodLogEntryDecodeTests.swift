import XCTest
@testable import FiboHealth

final class FoodLogEntryDecodeTests: XCTestCase {
    func test_decodesNewMacroFields() throws {
        let json = """
        {
          "id": 7, "food_name": "Chicken Breast", "weight_g": 200,
          "calories": 330, "is_healthy": 1, "health_score": 80,
          "timestamp": "2026-04-15T12:00:00",
          "protein_g": 62.0, "fat_g": 7.2, "sugar_g": 0.0, "fiber_g": 0.0
        }
        """.data(using: .utf8)!
        let entry = try JSONDecoder().decode(FoodLogEntry.self, from: json)
        XCTAssertEqual(entry.proteinG, 62.0)
        XCTAssertEqual(entry.fatG,     7.2)
        XCTAssertEqual(entry.sugarG,   0.0)
        XCTAssertEqual(entry.fiberG,   0.0)
    }

    func test_decodesLegacyPayloadWithoutMacros() throws {
        // Older Pi builds don't include macro keys — decoding must still succeed
        // (otherwise an OTA-mismatched Pi would break the entire food log sync).
        let json = """
        {
          "id": 7, "food_name": "Apple", "weight_g": 100,
          "calories": 52, "is_healthy": 1, "health_score": 75,
          "timestamp": "2026-04-15T12:00:00"
        }
        """.data(using: .utf8)!
        let entry = try JSONDecoder().decode(FoodLogEntry.self, from: json)
        XCTAssertNil(entry.proteinG)
        XCTAssertNil(entry.fatG)
        XCTAssertNil(entry.sugarG)
        XCTAssertNil(entry.fiberG)
    }
}
