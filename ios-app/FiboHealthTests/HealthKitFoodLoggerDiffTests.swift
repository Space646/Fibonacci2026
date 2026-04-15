import XCTest
@testable import FiboHealth

final class HealthKitFoodLoggerDiffTests: XCTestCase {
    func test_emptyInputs() {
        let r = HealthKitFoodLogger.diff(piIds: [], hkIds: [])
        XCTAssertTrue(r.toInsert.isEmpty)
        XCTAssertTrue(r.toDelete.isEmpty)
    }

    func test_allInsert_whenHKEmpty() {
        let r = HealthKitFoodLogger.diff(piIds: [1, 2, 3], hkIds: [])
        XCTAssertEqual(r.toInsert, [1, 2, 3])
        XCTAssertTrue(r.toDelete.isEmpty)
    }

    func test_allDelete_whenPiEmpty() {
        let r = HealthKitFoodLogger.diff(piIds: [], hkIds: [4, 5])
        XCTAssertTrue(r.toInsert.isEmpty)
        XCTAssertEqual(r.toDelete, [4, 5])
    }

    func test_mixed() {
        // 1 already in HK (kept), 2 is new (insert), 4 was deleted on Pi (delete from HK).
        let r = HealthKitFoodLogger.diff(piIds: [1, 2], hkIds: [1, 4])
        XCTAssertEqual(r.toInsert, [2])
        XCTAssertEqual(r.toDelete, [4])
    }
}
