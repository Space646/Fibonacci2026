# HealthKit Food Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Log Pi-scanned food to Apple HealthKit on iOS as `dietaryEnergyConsumed` + macro correlations, with deletions on the Pi cascading to HealthKit, gated by a `Settings.bundle` toggle (default OFF) and an HIG-compliant destructive removal action in `ProfileView`.

**Architecture:** Pi `get_todays_log` is extended to compute per-entry macro grams from the existing `foods` table JOIN. iOS gains a focused `HealthKitFoodLogger` service that uses HealthKit metadata (`pi_food_id`, `source_app`) as the single source of truth for the Pi↔HK mapping — reconciliation is a pure-function set diff between Pi ids and HK ids. `AppEnvironment` subscribes to `BluetoothClient.$foodLog` and `UserDefaults.didChangeNotification` to drive reconcile calls, gated by a `@AppStorage` toggle backed by a system `Settings.bundle`.

**Tech Stack:** Swift / SwiftUI / HealthKit / Combine on iOS; Python / SQLite / `bless` on Pi. Xcode project managed by XcodeGen (`ios-app/project.yml`).

**Spec:** [docs/superpowers/specs/2026-04-15-healthkit-food-logging-design.md](../specs/2026-04-15-healthkit-food-logging-design.md)

---

## File Structure

**Pi (Python):**
- Modify: `pi-app/services/user_session.py:68-80` — extend `get_todays_log` to add `protein_g`/`fat_g`/`sugar_g`/`fiber_g` per row.
- Modify: `pi-app/tests/test_user_session.py` — add coverage for the new fields.

**iOS (Swift):**
- Modify: `ios-app/FiboHealth/Models/FoodLogEntry.swift` — add four optional macro fields with permissive decoding.
- Create: `ios-app/FiboHealth/Services/HealthKitFoodLogger.swift` — new service (auth, reconcile, removal, pure diff helper).
- Modify: `ios-app/FiboHealth/Services/HealthKitService.swift:11-31` — add `dietaryEnergyConsumed` + macros to share set.
- Modify: `ios-app/FiboHealth/AppEnvironment.swift` — own logger; subscribe to `bluetooth.$foodLog` and `UserDefaults.didChangeNotification`.
- Modify: `ios-app/FiboHealth/Views/ProfileView.swift` — add "Health Integration" section + destructive alert.
- Create: `ios-app/FiboHealth/Settings.bundle/Root.plist` — system Settings toggle.
- Modify: `ios-app/project.yml` — add Settings.bundle to resources, update `NSHealthUpdateUsageDescription`.

**iOS Tests:**
- Create: `ios-app/FiboHealthTests/HealthKitFoodLoggerDiffTests.swift`.
- Create: `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift`.

---

## Task 1: Pi — extend `get_todays_log` with macro grams

**Files:**
- Modify: `pi-app/services/user_session.py:68-80`
- Test: `pi-app/tests/test_user_session.py`

- [ ] **Step 1: Write the failing test**

Append to `pi-app/tests/test_user_session.py`:

```python
def test_get_todays_log_includes_macro_grams(tmp_path, monkeypatch):
    """Each entry must carry per-entry gram values for protein/fat/sugar/fiber,
    computed as (per_100g * weight_g / 100). iOS uses these to build HealthKit
    correlation samples; missing keys would cause those samples to be omitted."""
    from pi_app.database.db import get_connection, run_migrations
    from pi_app.services.user_session import UserSession

    db_path = tmp_path / "test.db"
    monkeypatch.setenv("FIBO_DB_PATH", str(db_path))
    run_migrations()
    conn = get_connection()

    # Seed: one user, one food (Chicken Breast: 31g protein, 3.6g fat per 100g).
    conn.execute(
        "INSERT INTO users (id, bluetooth_mac, name) VALUES (1, 'aa:bb', 'T')"
    )
    conn.execute(
        """INSERT INTO foods
           (id, name, calories_per_100g, protein_per_100g, fat_per_100g,
            sugar_per_100g, fiber_per_100g, is_healthy, health_score)
           VALUES (1, 'Chicken Breast', 165, 31.0, 3.6, 0.0, 0.0, 1, 80)"""
    )
    conn.commit()

    session = UserSession(conn)
    session.add_log_entry(user_id=1, food_id=1, weight_g=200.0, calories=330.0)

    rows = session.get_todays_log(1)
    assert len(rows) == 1
    row = rows[0]
    assert row["protein_g"] == 62.0   # 31.0 * 200 / 100
    assert row["fat_g"]     == 7.2    # 3.6  * 200 / 100
    assert row["sugar_g"]   == 0.0
    assert row["fiber_g"]   == 0.0
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd pi-app && python -m pytest tests/test_user_session.py::test_get_todays_log_includes_macro_grams -v`
Expected: FAIL with `KeyError: 'protein_g'`.

- [ ] **Step 3: Implement — extend `get_todays_log` to compute the gram values**

In `pi-app/services/user_session.py`, replace the body of `get_todays_log` (lines 68-80) with:

```python
    def get_todays_log(self, user_id: int) -> list[dict]:
        rows = self._conn.execute(
            """SELECT fl.*, f.name as food_name, f.is_healthy, f.health_score,
                      f.protein_per_100g, f.fat_per_100g,
                      f.sugar_per_100g, f.fiber_per_100g
               FROM food_log fl
               JOIN foods f ON fl.food_id = f.id
               WHERE fl.user_id = ?
                 AND date(fl.timestamp) = date('now')
               ORDER BY fl.timestamp DESC""",
            (user_id,),
        ).fetchall()
        result = []
        for r in rows:
            d = dict(r)
            w = d.get("weight_g") or 0.0
            # Per-entry grams used by iOS to build HealthKit correlation
            # samples. Round to 2dp to keep BLE payload tight.
            d["protein_g"] = round((d.pop("protein_per_100g") or 0.0) * w / 100.0, 2)
            d["fat_g"]     = round((d.pop("fat_per_100g")     or 0.0) * w / 100.0, 2)
            d["sugar_g"]   = round((d.pop("sugar_per_100g")   or 0.0) * w / 100.0, 2)
            d["fiber_g"]   = round((d.pop("fiber_per_100g")   or 0.0) * w / 100.0, 2)
            result.append(d)
        return result
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd pi-app && python -m pytest tests/test_user_session.py -v`
Expected: PASS (the new test plus all existing user_session tests).

- [ ] **Step 5: Commit**

```bash
git add pi-app/services/user_session.py pi-app/tests/test_user_session.py
git commit -m "feat(pi): include per-entry macro grams in todays_log

iOS HealthKit food logging needs protein/fat/sugar/fiber grams per
entry to build food correlations. Compute them from the existing
foods JOIN (per_100g * weight_g / 100), rounded to 2dp."
```

---

## Task 2: iOS — extend `FoodLogEntry` with optional macro fields

**Files:**
- Modify: `ios-app/FiboHealth/Models/FoodLogEntry.swift`
- Create: `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift`

- [ ] **Step 1: Write the failing test**

Create `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift`:

```swift
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
```

- [ ] **Step 2: Regenerate Xcode project & run test to verify it fails**

```bash
cd ios-app && xcodegen generate
xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:FiboHealthTests/FoodLogEntryDecodeTests 2>&1 | tail -20
```
Expected: FAIL with "value of type 'FoodLogEntry' has no member 'proteinG'".

- [ ] **Step 3: Add the four optional fields to `FoodLogEntry`**

In `ios-app/FiboHealth/Models/FoodLogEntry.swift`, modify the struct to add the new properties, CodingKeys, and decoding. Final file:

```swift
import Foundation

struct FoodLogEntry: Codable, Identifiable {
    var id: Int
    var foodName: String
    var weightG: Double
    var calories: Double
    var isHealthy: Bool
    var healthScore: Double
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
        case healthScore = "health_score"
        case timestamp
        case proteinG    = "protein_g"
        case fatG        = "fat_g"
        case sugarG      = "sugar_g"
        case fiberG      = "fiber_g"
    }

    init(id: Int, foodName: String, weightG: Double, calories: Double,
         isHealthy: Bool, healthScore: Double, timestamp: String,
         proteinG: Double? = nil, fatG: Double? = nil,
         sugarG: Double? = nil, fiberG: Double? = nil) {
        self.id = id
        self.foodName = foodName
        self.weightG = weightG
        self.calories = calories
        self.isHealthy = isHealthy
        self.healthScore = healthScore
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
        healthScore = try c.decodeIfPresent(Double.self, forKey: .healthScore) ?? 0
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
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:FiboHealthTests/FoodLogEntryDecodeTests 2>&1 | tail -20
```
Expected: PASS for both `test_decodesNewMacroFields` and `test_decodesLegacyPayloadWithoutMacros`.

- [ ] **Step 5: Commit**

```bash
git add ios-app/FiboHealth/Models/FoodLogEntry.swift \
        ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift \
        ios-app/FiboHealth.xcodeproj/project.pbxproj
git commit -m "feat(ios): decode optional macro grams on FoodLogEntry"
```

---

## Task 3: iOS — pure `diff` helper for `HealthKitFoodLogger`

Build the deterministic, unit-testable core of the reconciler before any HealthKit-talking code.

**Files:**
- Create: `ios-app/FiboHealth/Services/HealthKitFoodLogger.swift`
- Create: `ios-app/FiboHealthTests/HealthKitFoodLoggerDiffTests.swift`

- [ ] **Step 1: Write the failing test**

Create `ios-app/FiboHealthTests/HealthKitFoodLoggerDiffTests.swift`:

```swift
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
```

- [ ] **Step 2: Create the file with just enough to fail**

Create `ios-app/FiboHealth/Services/HealthKitFoodLogger.swift`:

```swift
import HealthKit
import Combine
import Foundation

final class HealthKitFoodLogger: ObservableObject {
    // Reconciliation core — pure, testable.
    static func diff(piIds: Set<Int>, hkIds: Set<Int>)
        -> (toInsert: Set<Int>, toDelete: Set<Int>)
    {
        // intentionally wrong to make the test fail first
        return (toInsert: [], toDelete: [])
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
cd ios-app && xcodegen generate && xcodebuild test \
  -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:FiboHealthTests/HealthKitFoodLoggerDiffTests 2>&1 | tail -25
```
Expected: 3 of 4 tests FAIL (`test_emptyInputs` happens to pass).

- [ ] **Step 4: Implement the diff**

Replace the body of `diff` in `HealthKitFoodLogger.swift`:

```swift
    static func diff(piIds: Set<Int>, hkIds: Set<Int>)
        -> (toInsert: Set<Int>, toDelete: Set<Int>)
    {
        return (toInsert: piIds.subtracting(hkIds),
                toDelete: hkIds.subtracting(piIds))
    }
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:FiboHealthTests/HealthKitFoodLoggerDiffTests 2>&1 | tail -10
```
Expected: PASS (4/4).

- [ ] **Step 6: Commit**

```bash
git add ios-app/FiboHealth/Services/HealthKitFoodLogger.swift \
        ios-app/FiboHealthTests/HealthKitFoodLoggerDiffTests.swift \
        ios-app/FiboHealth.xcodeproj/project.pbxproj
git commit -m "feat(ios): pure diff core for HealthKitFoodLogger"
```

---

## Task 4: iOS — extend `HealthKitService` write authorization set

The existing `requestAuthorization` requests an empty share set; nutrition writes will silently fail unless the types are in the share set when the HK auth sheet is shown.

**Files:**
- Modify: `ios-app/FiboHealth/Services/HealthKitService.swift:11-31`

- [ ] **Step 1: Add nutrition types to `readTypes` block as a new `shareTypes` set, and pass it to `requestAuthorization`**

In `ios-app/FiboHealth/Services/HealthKitService.swift`, replace the `readTypes` block (lines 11-21) and the `requestAuthorization()` method (lines 23-31) with:

```swift
    private let readTypes: Set<HKObjectType> = {
        var types = Set<HKObjectType>()
        let ids: [HKQuantityTypeIdentifier] = [
            .stepCount, .activeEnergyBurned, .basalEnergyBurned, .appleExerciseTime
        ]
        for id in ids {
            if let t = HKQuantityType.quantityType(forIdentifier: id) { types.insert(t) }
        }
        types.insert(HKObjectType.workoutType())
        return types
    }()

    // Nutrition write set — used for the food-logging feature. Calories +
    // four macros (carbohydrates intentionally omitted; see spec).
    static let nutritionShareTypes: Set<HKSampleType> = {
        var types = Set<HKSampleType>()
        let ids: [HKQuantityTypeIdentifier] = [
            .dietaryEnergyConsumed, .dietaryProtein, .dietaryFatTotal,
            .dietarySugar, .dietaryFiber
        ]
        for id in ids {
            if let t = HKQuantityType.quantityType(forIdentifier: id) { types.insert(t) }
        }
        return types
    }()

    func requestAuthorization() {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        store.requestAuthorization(toShare: HealthKitService.nutritionShareTypes,
                                   read: readTypes) { [weak self] success, _ in
            DispatchQueue.main.async {
                self?.isAuthorized = success
                if success { self?.fetchToday() }
            }
        }
    }
```

- [ ] **Step 2: Build to verify**

```bash
cd ios-app && xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' 2>&1 | tail -10
```
Expected: BUILD SUCCEEDED.

- [ ] **Step 3: Commit**

```bash
git add ios-app/FiboHealth/Services/HealthKitService.swift
git commit -m "feat(ios): include nutrition types in HK write auth request"
```

---

## Task 5: iOS — `HealthKitFoodLogger` reconcile + remove implementation

Adds the HealthKit-talking surface: build correlations, query existing entries by metadata, save/delete to apply the diff, and a remove-all helper for the destructive button.

**Files:**
- Modify: `ios-app/FiboHealth/Services/HealthKitFoodLogger.swift` (file from Task 3)

- [ ] **Step 1: Replace the file with the full implementation**

Overwrite `ios-app/FiboHealth/Services/HealthKitFoodLogger.swift`:

```swift
import HealthKit
import Combine
import Foundation

/// Reconciles the Pi's food log into HealthKit food correlations.
///
/// Source of truth for the Pi↔HK mapping is HealthKit metadata
/// (`pi_food_id` + `source_app == "FiboHealth"`) — there is no local cache,
/// so deletions on the Pi cascade cleanly to HealthKit.
final class HealthKitFoodLogger: ObservableObject {
    static let metaPiId      = "pi_food_id"
    static let metaSourceApp = "source_app"
    static let sourceAppValue = "FiboHealth"

    @Published var lastError: String?

    private let store = HKHealthStore()

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withFullDate, .withTime, .withColonSeparatorInTime,
                           .withDashSeparatorInDate]
        return f
    }()

    // MARK: - Pure core (tested in HealthKitFoodLoggerDiffTests)

    static func diff(piIds: Set<Int>, hkIds: Set<Int>)
        -> (toInsert: Set<Int>, toDelete: Set<Int>)
    {
        return (toInsert: piIds.subtracting(hkIds),
                toDelete: hkIds.subtracting(piIds))
    }

    // MARK: - Authorization

    func requestWriteAuthorization() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        do {
            try await store.requestAuthorization(
                toShare: HealthKitService.nutritionShareTypes, read: []
            )
        } catch {
            await MainActor.run { self.lastError = error.localizedDescription }
        }
    }

    // MARK: - Reconciliation

    /// Add HK correlations for entries newly seen on the Pi; delete HK
    /// correlations whose `pi_food_id` no longer appears in the Pi log.
    func reconcile(with entries: [FoodLogEntry]) async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return }

        let existing: [HKCorrelation]
        do {
            existing = try await fetchFiboHealthCorrelations(of: foodCorrelationType)
        } catch {
            await MainActor.run { self.lastError = "HK query failed: \(error.localizedDescription)" }
            return
        }

        let hkIds: Set<Int> = Set(existing.compactMap {
            ($0.metadata?[Self.metaPiId] as? String).flatMap(Int.init)
        })
        let piIds: Set<Int> = Set(entries.map { $0.id })
        let (toInsert, toDelete) = Self.diff(piIds: piIds, hkIds: hkIds)

        // Inserts — build correlation per new entry.
        let entriesById = Dictionary(uniqueKeysWithValues: entries.map { ($0.id, $0) })
        for id in toInsert {
            guard let entry = entriesById[id],
                  let correlation = buildCorrelation(for: entry) else { continue }
            do { try await store.save(correlation) }
            catch {
                await MainActor.run {
                    self.lastError = "HK save failed for id \(id): \(error.localizedDescription)"
                }
            }
        }

        // Deletes — find correlations matching pi ids in toDelete.
        let toDeleteCorrelations = existing.filter {
            guard let s = $0.metadata?[Self.metaPiId] as? String,
                  let i = Int(s) else { return false }
            return toDelete.contains(i)
        }
        for c in toDeleteCorrelations {
            do { try await store.delete(c) }
            catch {
                await MainActor.run {
                    self.lastError = "HK delete failed: \(error.localizedDescription)"
                }
            }
        }
    }

    /// Delete every food correlation this app has written. Returns counts.
    func removeAllFiboHealthEntries() async -> (removed: Int, failed: Int) {
        guard HKHealthStore.isHealthDataAvailable() else { return (0, 0) }
        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return (0, 0) }

        let existing: [HKCorrelation]
        do { existing = try await fetchFiboHealthCorrelations(of: foodCorrelationType) }
        catch { return (0, 0) }

        var removed = 0
        var failed = 0
        for c in existing {
            do { try await store.delete(c); removed += 1 }
            catch { failed += 1 }
        }
        return (removed, failed)
    }

    // MARK: - Internals

    private func fetchFiboHealthCorrelations(of type: HKCorrelationType)
        async throws -> [HKCorrelation]
    {
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: Self.metaSourceApp,
            operatorType: .equalTo,
            value: Self.sourceAppValue
        )
        return try await withCheckedThrowingContinuation { cont in
            let q = HKSampleQuery(
                sampleType: type, predicate: predicate,
                limit: HKObjectQueryNoLimit, sortDescriptors: nil
            ) { _, samples, error in
                if let error = error { cont.resume(throwing: error); return }
                cont.resume(returning: (samples as? [HKCorrelation]) ?? [])
            }
            store.execute(q)
        }
    }

    /// Returns nil if the entry can't be turned into a correlation (no
    /// parseable date or no calories).
    private func buildCorrelation(for entry: FoodLogEntry) -> HKCorrelation? {
        guard let date = Self.isoFormatter.date(from: entry.timestamp) else {
            return nil
        }
        let metadata: [String: Any] = [
            Self.metaPiId:      String(entry.id),
            Self.metaSourceApp: Self.sourceAppValue,
            HKMetadataKeyFoodType: entry.foodName,
        ]

        // Calories required.
        guard let kcalType = HKQuantityType.quantityType(
            forIdentifier: .dietaryEnergyConsumed
        ) else { return nil }
        var members = Set<HKSample>()
        members.insert(HKQuantitySample(
            type: kcalType,
            quantity: HKQuantity(unit: .kilocalorie(), doubleValue: entry.calories),
            start: date, end: date, metadata: metadata
        ))

        // Macros — written even when 0; only nil/missing values are skipped.
        func add(_ id: HKQuantityTypeIdentifier, _ grams: Double?) {
            guard let g = grams,
                  let t = HKQuantityType.quantityType(forIdentifier: id) else { return }
            members.insert(HKQuantitySample(
                type: t,
                quantity: HKQuantity(unit: .gram(), doubleValue: g),
                start: date, end: date, metadata: metadata
            ))
        }
        add(.dietaryProtein,  entry.proteinG)
        add(.dietaryFatTotal, entry.fatG)
        add(.dietarySugar,    entry.sugarG)
        add(.dietaryFiber,    entry.fiberG)

        guard let foodCorrelationType = HKObjectType.correlationType(
            forIdentifier: .food
        ) else { return nil }
        return HKCorrelation(
            type: foodCorrelationType,
            start: date, end: date,
            objects: members, metadata: metadata
        )
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd ios-app && xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' 2>&1 | tail -15
```
Expected: BUILD SUCCEEDED.

- [ ] **Step 3: Re-run diff tests to confirm the pure core still passes**

```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:FiboHealthTests/HealthKitFoodLoggerDiffTests 2>&1 | tail -10
```
Expected: PASS (4/4).

- [ ] **Step 4: Commit**

```bash
git add ios-app/FiboHealth/Services/HealthKitFoodLogger.swift
git commit -m "feat(ios): HK food correlation reconcile + remove-all"
```

---

## Task 6: iOS — `Settings.bundle` with `Log Food to Health` toggle

A `Settings.bundle` is a folder named `Settings.bundle` (Xcode treats the suffix as a bundle) containing a `Root.plist`. iOS surfaces it under Settings → FiboHealth.

**Files:**
- Create: `ios-app/FiboHealth/Settings.bundle/Root.plist`
- Modify: `ios-app/project.yml`

- [ ] **Step 1: Create `Root.plist`**

Create `ios-app/FiboHealth/Settings.bundle/Root.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>StringsTable</key>
    <string>Root</string>
    <key>PreferenceSpecifiers</key>
    <array>
        <dict>
            <key>Type</key>
            <string>PSGroupSpecifier</string>
            <key>Title</key>
            <string>Apple Health</string>
            <key>FooterText</key>
            <string>When on, food scans from your Pi are logged to Health as Dietary Energy and macronutrients. To remove previously logged entries, open FiboHealth → Profile.</string>
        </dict>
        <dict>
            <key>Type</key>
            <string>PSToggleSwitchSpecifier</string>
            <key>Title</key>
            <string>Log Food to Health</string>
            <key>Key</key>
            <string>healthkit_food_logging_enabled</string>
            <key>DefaultValue</key>
            <false/>
            <key>TrueValue</key>
            <true/>
            <key>FalseValue</key>
            <false/>
        </dict>
    </array>
</dict>
</plist>
```

- [ ] **Step 2: Update `project.yml` to register the Settings bundle as a resource and to fix the HK update usage description**

In `ios-app/project.yml`:

Replace line 31:
```
        NSHealthUpdateUsageDescription: "FiboHealth does not write health data."
```
with:
```
        NSHealthUpdateUsageDescription: "FiboHealth logs the food you scan on your Pi as dietary energy and macronutrients in Apple Health, so your daily calorie balance stays accurate."
```

Replace lines 19-21 (the `sources:` block):
```
    sources:
      - path: FiboHealth
        createIntermediateGroups: true
```
with:
```
    sources:
      - path: FiboHealth
        createIntermediateGroups: true
        excludes:
          - "Settings.bundle"
      - path: FiboHealth/Settings.bundle
        type: bundle
        buildPhase: resources
```

(`excludes` prevents XcodeGen from walking into the bundle as if it were a Swift source folder; the second entry registers the bundle itself as a copy-resources build phase input.)

- [ ] **Step 3: Regenerate Xcode project & build**

```bash
cd ios-app && xcodegen generate
xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' 2>&1 | tail -15
```
Expected: BUILD SUCCEEDED. Confirm in the build log that `Settings.bundle` appears in a `CpResource` step.

- [ ] **Step 4: Verify on simulator**

```bash
cd ios-app && xcodebuild -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' install 2>&1 | tail -5
xcrun simctl launch booted com.fibonacci.fibohealth
```
Then in the simulator: open the system **Settings** app → scroll to **FiboHealth** → confirm "Apple Health" group with "Log Food to Health" toggle (off) and the footer text.

- [ ] **Step 5: Commit**

```bash
git add ios-app/FiboHealth/Settings.bundle/Root.plist ios-app/project.yml \
        ios-app/FiboHealth.xcodeproj/project.pbxproj
git commit -m "feat(ios): Settings.bundle toggle for Log Food to Health"
```

---

## Task 7: iOS — wire `HealthKitFoodLogger` into `AppEnvironment`

Subscribes the logger to `bluetooth.$foodLog` (so reconcile runs on every fresh Pi sync) and to `UserDefaults.didChangeNotification` (so flipping the toggle in system Settings while the app is foregrounded immediately re-requests auth and reconciles). Both paths gated by the toggle key.

**Files:**
- Modify: `ios-app/FiboHealth/AppEnvironment.swift`
- Modify: `ios-app/FiboHealth/FiboHealthApp.swift` (only if `HealthKitFoodLogger` needs to be injected as `@EnvironmentObject` — check first)

- [ ] **Step 1: Check whether the logger needs to be an `@EnvironmentObject`**

Open `ios-app/FiboHealth/FiboHealthApp.swift` and check the existing `.environmentObject(...)` chain. If `HealthKitService` is injected, the logger should be too (so `ProfileView` can call `removeAllFiboHealthEntries()`).

Run: `grep -n environmentObject ios-app/FiboHealth/FiboHealthApp.swift`
If `healthKit` is injected, plan to inject `healthKitFoodLogger` similarly in Step 3.

- [ ] **Step 2: Replace `AppEnvironment.swift`**

Overwrite `ios-app/FiboHealth/AppEnvironment.swift`:

```swift
import SwiftUI
import SwiftData
import Combine

@MainActor
final class AppEnvironment: ObservableObject {
    @Published var theme: Theme = .dark

    let profileStore: UserProfileStore
    let healthKit: HealthKitService
    let healthKitFoodLogger: HealthKitFoodLogger
    let bluetooth: BluetoothClient

    static let healthLoggingEnabledKey = "healthkit_food_logging_enabled"

    private var cancellables = Set<AnyCancellable>()

    init(modelContext: ModelContext) {
        self.profileStore = UserProfileStore(modelContext: modelContext)
        self.healthKit = HealthKitService()
        self.healthKitFoodLogger = HealthKitFoodLogger()
        self.bluetooth = BluetoothClient()

        // Wire up: BLE pending payloads start from the current profile + snapshot
        self.bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        self.bluetooth.pendingProfile = profileStore.profile.blePayload()
        self.bluetooth.pendingSnapshot = healthKit.snapshot

        // Request HealthKit on startup
        self.healthKit.requestAuthorization()

        // Reconcile food log → HK whenever the Pi sends a fresh log,
        // gated by the system Settings toggle.
        self.bluetooth.$foodLog
            .receive(on: DispatchQueue.main)
            .sink { [weak self] entries in
                self?.reconcileIfEnabled(entries: entries)
            }
            .store(in: &cancellables)

        // React to the toggle being flipped in Settings while we're foregrounded.
        NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.handleToggleChangeIfNeeded()
            }
            .store(in: &cancellables)
    }

    /// Push the latest profile + health snapshot to the Pi. If already
    /// connected, writes immediately; otherwise updates the pending payloads
    /// which will be sent on next connect.
    func syncToPi() {
        bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        bluetooth.pendingProfile = profileStore.profile.blePayload()
        bluetooth.pendingSnapshot = healthKit.snapshot
        bluetooth.resync()
    }

    // MARK: - HK food logging coordination

    private var lastSeenToggle: Bool = UserDefaults.standard
        .bool(forKey: AppEnvironment.healthLoggingEnabledKey)

    private func reconcileIfEnabled(entries: [FoodLogEntry]) {
        guard UserDefaults.standard.bool(forKey: Self.healthLoggingEnabledKey) else { return }
        Task { await healthKitFoodLogger.reconcile(with: entries) }
    }

    private func handleToggleChangeIfNeeded() {
        let now = UserDefaults.standard.bool(forKey: Self.healthLoggingEnabledKey)
        guard now != lastSeenToggle else { return }
        lastSeenToggle = now
        if now {
            // Just turned ON — ensure write auth, then reconcile current log.
            let entries = bluetooth.foodLog
            Task {
                await healthKitFoodLogger.requestWriteAuthorization()
                await healthKitFoodLogger.reconcile(with: entries)
            }
        }
        // Toggling OFF: do nothing destructive — user must use the in-app
        // "Remove FiboHealth Food Entries" button.
    }
}
```

- [ ] **Step 3: Inject `healthKitFoodLogger` as an `@EnvironmentObject` (if HK is injected today)**

If Step 1 showed `healthKit` being injected in `FiboHealthApp.swift`, find that injection and add a sibling line:

```swift
.environmentObject(env.healthKitFoodLogger)
```

- [ ] **Step 4: Build to verify**

```bash
cd ios-app && xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' 2>&1 | tail -10
```
Expected: BUILD SUCCEEDED.

- [ ] **Step 5: Commit**

```bash
git add ios-app/FiboHealth/AppEnvironment.swift ios-app/FiboHealth/FiboHealthApp.swift
git commit -m "feat(ios): drive HK food reconcile from BLE + Settings toggle"
```

---

## Task 8: iOS — `ProfileView` "Health Integration" section + destructive alert

**Files:**
- Modify: `ios-app/FiboHealth/Views/ProfileView.swift`

- [ ] **Step 1: Add the new section, alert state, and confirmation logic**

In `ios-app/FiboHealth/Views/ProfileView.swift`:

a) Add `@EnvironmentObject` for the logger and `@AppStorage` for the toggle, plus alert state — at the top of the `ProfileView` struct, just below the existing `@EnvironmentObject var env`:

```swift
    @EnvironmentObject var healthKitFoodLogger: HealthKitFoodLogger
    @AppStorage(AppEnvironment.healthLoggingEnabledKey) private var healthLoggingEnabled: Bool = false

    @State private var showRemoveConfirm = false
    @State private var showRemoveResult = false
    @State private var removeResultMessage = ""
```

b) Add a new `Section` to the `Form`, immediately above the existing `Save & Sync to Pi` section (which is the last `Section { Button("Save & Sync to Pi") ... }` block):

```swift
                Section("Health Integration") {
                    HStack {
                        Text("Log Food to Health")
                            .foregroundColor(env.theme.textPrimary)
                        Spacer()
                        Image(systemName: healthLoggingEnabled
                              ? "checkmark.circle.fill" : "xmark.circle")
                            .foregroundColor(healthLoggingEnabled ? .green : env.theme.textMuted)
                    }
                    Text("Manage in Settings → FiboHealth.")
                        .foregroundColor(env.theme.textMuted)
                        .font(.system(size: 12))
                    if let err = healthKitFoodLogger.lastError {
                        Text(err)
                            .foregroundColor(env.theme.textMuted)
                            .font(.system(size: 11))
                    }
                    Button(role: .destructive) {
                        showRemoveConfirm = true
                    } label: {
                        Text("Remove FiboHealth Food Entries from Health")
                    }
                }
```

c) Add the alert modifiers — attach to the `Form` (or any view in its tree; place after `.navigationTitle("Profile")`):

```swift
            .alert("Remove Food Entries from Health?",
                   isPresented: $showRemoveConfirm) {
                Button("Cancel", role: .cancel) { }
                Button("Remove", role: .destructive) {
                    Task {
                        let r = await healthKitFoodLogger.removeAllFiboHealthEntries()
                        await MainActor.run {
                            removeResultMessage = r.failed == 0
                                ? "Removed \(r.removed) entries."
                                : "Removed \(r.removed) entries, \(r.failed) failed."
                            showRemoveResult = true
                        }
                    }
                }
            } message: {
                Text("This will permanently delete all food entries FiboHealth has written to Apple Health. Activity data and entries from other apps are not affected. This cannot be undone.")
            }
            .alert("Done", isPresented: $showRemoveResult) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(removeResultMessage)
            }
```

- [ ] **Step 2: Build & launch on simulator**

```bash
cd ios-app && xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15' 2>&1 | tail -10
```
Expected: BUILD SUCCEEDED.

- [ ] **Step 3: Visual sanity check**

Launch the app on the simulator (iPhone 15). Open Profile tab. Confirm:
- "Health Integration" section appears above "Save & Sync to Pi".
- Status icon is muted X (toggle is OFF by default).
- Tapping "Remove FiboHealth Food Entries from Health" shows the destructive alert with a red **Remove** button (SwiftUI honours `role: .destructive` in HIG-standard red).
- Cancelling dismisses without action.

- [ ] **Step 4: Commit**

```bash
git add ios-app/FiboHealth/Views/ProfileView.swift
git commit -m "feat(ios): Health Integration section + destructive remove alert"
```

---

## Task 9: End-to-end manual verification on hardware

HealthKit Simulator support is limited — sample writes work but the Health app isn't installed on simulators in older Xcode versions. Run this on a physical iPhone paired with a running Pi.

**Files:** none

- [ ] **Step 1: Fresh-install OFF path**

Delete the app from the device. Reinstall via Xcode. **Do not** flip the toggle. Scan a food item on the Pi. Confirm:
- The entry appears in FiboHealth → Food Log.
- The entry **does not** appear in Health → Browse → Nutrition → Dietary Energy.

- [ ] **Step 2: Toggle ON path**

iOS Settings → FiboHealth → flip "Log Food to Health" ON. Return to FiboHealth. iOS will present a HealthKit auth sheet covering the five nutrition types — tap "Turn On All" → Allow. Re-scan a food item on the Pi. Confirm:
- Entry appears in Health → Browse → Nutrition → **Dietary Energy** with the right kcal value.
- Same entry's macros appear under **Protein**, **Total Fat**, **Sugar**, **Fiber** (chicken breast: ~62g protein for 200g; apple: ~0g fat).
- Tapping the entry shows source as "FiboHealth".

- [ ] **Step 3: Cascade-delete path**

In FiboHealth or on the Pi, delete the food entry from the Pi log. Within one BLE sync cycle (or after pull-to-refresh on Food Log), confirm the corresponding Health entries are gone from Dietary Energy / Protein / Fat / Sugar / Fiber.

- [ ] **Step 4: Toggle OFF — entries persist**

Flip toggle OFF. Confirm prior Health entries remain in place (no auto-delete).

- [ ] **Step 5: Destructive removal path**

Open FiboHealth → Profile → "Remove FiboHealth Food Entries from Health". Confirm:
- Alert title and message match the spec verbatim.
- The **Remove** button is red.
- Cancelling does nothing.
- Confirming triggers the follow-up "Removed N entries." alert.
- All FiboHealth entries are gone from the Health app's nutrition rows.
- Activity data (steps, workouts) is **untouched**.

- [ ] **Step 6: Re-toggle ON re-inserts current log**

Flip toggle ON again. Within one sync cycle, confirm today's Pi entries appear in Health again.

- [ ] **Step 7: Commit verification notes (optional)**

If anything required a code tweak during verification, commit it now with a `fix(...)` message.

---

## Self-Review Summary

- **Spec coverage:** all sections (Settings.bundle toggle, ProfileView destructive button, HK correlation shape, Pi macro extension, reconcile algorithm, error handling, testing plan, scope exclusions) are addressed by Tasks 1–9.
- **No placeholders:** every step has concrete code or commands.
- **Type consistency:** `HealthKitService.nutritionShareTypes` is referenced by both the existing `requestAuthorization` (Task 4) and the new logger (Task 5); `AppEnvironment.healthLoggingEnabledKey` is the single source for the `UserDefaults`/`@AppStorage` key (Tasks 6, 7, 8); `pi_food_id` / `source_app` metadata keys are constants on `HealthKitFoodLogger` (Task 5).
- **Carbs gap:** intentionally not written; spec out-of-scope item explicitly addressed.
