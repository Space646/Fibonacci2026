import XCTest
@testable import FiboHealth

final class FiboHealthTests: XCTestCase {
    func testUserProfileBLEPayloadIncludesDeviceId() throws {
        let profile = UserProfile(name: "Alice", age: 30,
                                  weightKg: 60, heightCm: 165,
                                  sex: "female",
                                  activityLevel: "moderate",
                                  dailyCalorieGoal: nil)
        let payload = profile.blePayload()

        let deviceIdValue = payload["device_id"]
        XCTAssertNotNil(deviceIdValue, "blePayload must include device_id")
        guard let str = deviceIdValue as? String else {
            XCTFail("device_id should be a String (UUID)")
            return
        }
        XCTAssertNotNil(UUID(uuidString: str),
                        "device_id must be a valid UUID string")
    }

    func testUserProfileBLEPayloadContainsAllFields() throws {
        let profile = UserProfile(name: "Alice", age: 30,
                                  weightKg: 60, heightCm: 165,
                                  sex: "female",
                                  activityLevel: "moderate",
                                  dailyCalorieGoal: 2000)
        let payload = profile.blePayload()
        XCTAssertEqual(payload["name"] as? String, "Alice")
        XCTAssertEqual(payload["age"] as? Int, 30)
        XCTAssertEqual(payload["weight_kg"] as? Double, 60)
        XCTAssertEqual(payload["height_cm"] as? Double, 165)
        XCTAssertEqual(payload["sex"] as? String, "female")
        XCTAssertEqual(payload["activity_level"] as? String, "moderate")
        XCTAssertEqual(payload["daily_calorie_goal"] as? Double, 2000)
    }
}
