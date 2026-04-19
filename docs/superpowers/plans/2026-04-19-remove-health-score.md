# Remove Health Score Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the numeric `health_score` field from every platform (Pi, Android, iOS) while keeping `is_healthy` / Healthy/Unhealthy status untouched.

**Architecture:** Each platform's data model is updated independently — schema first on the Pi, then the Python service layer and QML UI, then the Android and iOS model files, then tests. No database migration is needed; this only affects fresh installs.

**Tech Stack:** Python 3 + SQLite (Pi), QML/Qt (Pi UI), Kotlin + kotlinx.serialization (Android), Swift + Codable (iOS)

---

## Files Modified

| File | Change |
|---|---|
| `pi-app/database/schema.sql` | Drop `health_score` column from `foods` table |
| `pi-app/database/seed_foods.py` | Remove `compute_health_score()` and its call site |
| `pi-app/services/user_session.py` | Remove `f.health_score` from SELECT |
| `pi-app/ui/screens/ScanResult.qml` | Remove Health Score bar Row (lines 112–135) |
| `pi-app/tests/test_database.py` | Remove `health_score` from column assertion; delete score tests |
| `android-app/.../data/model/FoodLogEntry.kt` | Remove `healthScore: Float` field + deserializer line |
| `android-app/.../FoodLogEntryDecoderTest.kt` | Remove `health_score` from test JSON |
| `ios-app/FiboHealth/Models/FoodLogEntry.swift` | Remove `healthScore` property, coding key, init param, decode line |
| `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift` | Remove `health_score` from test JSON |

---

### Task 1: Pi — Update schema.sql

**Files:**
- Modify: `pi-app/database/schema.sql:10`

- [ ] **Step 1: Remove the `health_score` column from the `foods` table**

Replace the current `foods` table definition. The file should look like this in full:

```sql
CREATE TABLE IF NOT EXISTS foods (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    name                TEXT NOT NULL,
    calories_per_100g   REAL NOT NULL,
    protein_per_100g    REAL DEFAULT 0,
    fat_per_100g        REAL DEFAULT 0,
    sugar_per_100g      REAL DEFAULT 0,
    fiber_per_100g      REAL DEFAULT 0,
    is_healthy          INTEGER DEFAULT 0,
    huskylens_label_id  INTEGER UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    bluetooth_mac       TEXT UNIQUE NOT NULL,
    name                TEXT,
    age                 INTEGER,
    weight_kg           REAL,
    height_cm           REAL,
    sex                 TEXT DEFAULT 'other',
    activity_level      TEXT DEFAULT 'sedentary',
    daily_calorie_goal  REAL
);

CREATE TABLE IF NOT EXISTS food_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id     INTEGER NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    weight_g    REAL NOT NULL,
    calories    REAL NOT NULL,
    timestamp   TEXT DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

CREATE TABLE IF NOT EXISTS health_snapshots (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date            TEXT NOT NULL,
    steps           INTEGER DEFAULT 0,
    calories_burned REAL DEFAULT 0,
    active_minutes  INTEGER DEFAULT 0,
    workouts        INTEGER DEFAULT 0,
    UNIQUE(user_id, date)
);
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/database/schema.sql
git commit -m "chore(db): remove health_score column from foods schema"
```

---

### Task 2: Pi — Update seed_foods.py

**Files:**
- Modify: `pi-app/database/seed_foods.py`

- [ ] **Step 1: Remove `compute_health_score()` and update `seed_foods()`**

Replace the entire file with:

```python
from database.db import get_connection, run_migrations


# (name, kcal/100g, protein, fat, sugar, fiber, is_healthy, huskylens_label_id)
FOODS = [
    ("Apple",          52,  0.3, 0.2, 10.0, 2.4, 1, 1),
    ("Banana",         89,  1.1, 0.3, 12.2, 2.6, 1, 2),
    ("Orange",         47,  0.9, 0.1,  9.4, 2.4, 1, 3),
    ("Broccoli",       34,  2.8, 0.4,  1.7, 2.6, 1, 4),
    ("Carrot",         41,  0.9, 0.2,  4.7, 2.8, 1, 5),
    ("Chicken Breast", 165, 31.0, 3.6,  0.0, 0.0, 1, 6),
    ("Egg",            155, 13.0, 11.0,  0.6, 0.0, 1, 7),
    ("White Rice",     130,  2.7, 0.3,  0.0, 0.4, 0, 8),
    ("Whole Wheat Bread", 247, 13.0, 3.5, 5.0, 6.0, 1, 9),
    ("Cheddar Cheese", 402, 25.0, 33.0,  0.1, 0.0, 0, 10),
    ("Chocolate Bar",  535,  4.9, 29.7, 56.9, 3.4, 0, 11),
    ("Potato Chips",   547,  6.5, 37.0,  0.4, 4.4, 0, 12),
    ("Salmon",         208, 20.0, 13.0,  0.0, 0.0, 1, 13),
    ("Greek Yogurt",    59,  10.0,  0.4,  3.2, 0.0, 1, 14),
    ("Avocado",        160,  2.0, 15.0,  0.7, 6.7, 1, 15),
    ("Almonds",        579, 21.0, 50.0,  4.4, 12.5, 1, 16),
    ("Oats",           389, 17.0,  7.0,  1.0, 10.6, 1, 17),
    ("White Bread",    265,  9.0,  3.2,  5.0,  2.7, 0, 18),
    ("Coca-Cola",       37,  0.0,  0.0, 10.6,  0.0, 0, 19),
    ("Butter",         717,  0.9, 81.0,  0.1,  0.0, 0, 20),
    ("Spinach",         23,  2.9,  0.4,  0.4,  2.2, 1, 21),
    ("Lentils",        116,  9.0,  0.4,  1.8,  7.9, 1, 22),
    ("Pizza (plain)",  266, 11.0, 10.0,  3.6,  2.3, 0, 23),
    ("Milk (whole)",    61,  3.2,  3.3,  4.8,  0.0, 1, 24),
    ("Strawberry",      32,  0.7,  0.3,  4.9,  2.0, 1, 25),
]


def seed_foods(conn):
    for (name, kcal, protein, fat, sugar, fiber, healthy, label_id) in FOODS:
        conn.execute(
            """INSERT OR IGNORE INTO foods
               (name, calories_per_100g, protein_per_100g, fat_per_100g,
                sugar_per_100g, fiber_per_100g, is_healthy,
                huskylens_label_id)
               VALUES (?,?,?,?,?,?,?,?)""",
            (name, kcal, protein, fat, sugar, fiber, healthy, label_id),
        )
    conn.commit()


if __name__ == "__main__":
    conn = get_connection()
    run_migrations(conn)
    seed_foods(conn)
    count = conn.execute("SELECT COUNT(*) FROM foods").fetchone()[0]
    print(f"Seeded {count} foods.")
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/database/seed_foods.py
git commit -m "chore(pi): remove compute_health_score and health_score from seed"
```

---

### Task 3: Pi — Update user_session.py

**Files:**
- Modify: `pi-app/services/user_session.py:70`

- [ ] **Step 1: Remove `f.health_score` from the SELECT**

In `get_todays_log`, change the query from:

```python
            """SELECT fl.*, f.name as food_name, f.is_healthy, f.health_score,
                      f.protein_per_100g, f.fat_per_100g,
                      f.sugar_per_100g, f.fiber_per_100g
```

to:

```python
            """SELECT fl.*, f.name as food_name, f.is_healthy,
                      f.protein_per_100g, f.fat_per_100g,
                      f.sugar_per_100g, f.fiber_per_100g
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/services/user_session.py
git commit -m "chore(pi): remove health_score from food log query"
```

---

### Task 4: Pi — Remove Health Score bar from ScanResult.qml

**Files:**
- Modify: `pi-app/ui/screens/ScanResult.qml:112-135`

- [ ] **Step 1: Delete the Health Score bar block**

Remove lines 112–135 (the `// Health score bar` comment and its `Row { ... }` block). The `Column` should end after the Fat `MacroBar`:

```qml
                    MacroBar { width: parent.width; label: "Fat"
                               value: detectedFood.fat_per_100g || 0; maxValue: 30
                               barColor: "#fbbf24"; isDark: root.isDark }
                }
            }
```

The deleted block is:

```qml
                    // Health score bar
                    Row {
                        width: parent.width
                        Text { width: 120; text: "Health Score"; font.pixelSize: 18; color: muted
                               verticalAlignment: Text.AlignVCenter; height: 24 }
                        Rectangle {
                            height: 8; width: parent.width - 120 - 60; radius: 4
                            anchors.verticalCenter: parent.verticalCenter
                            color: isDark ? "#0f172a" : "#e2e8f0"
                            Rectangle {
                                height: parent.height; radius: 3
                                width: ((detectedFood.health_score || 50) / 100) * parent.width
                                gradient: Gradient {
                                    orientation: Gradient.Horizontal
                                    GradientStop { position: 0.0; color: "#6366f1" }
                                    GradientStop { position: 1.0; color: "#06b6d4" }
                                }
                            }
                        }
                        Text { width: 60; text: (detectedFood.health_score || 50) + "/100"
                               font.pixelSize: 18; font.bold: true; color: "#06b6d4"
                               horizontalAlignment: Text.AlignRight
                               verticalAlignment: Text.AlignVCenter; height: 24 }
                    }
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/ui/screens/ScanResult.qml
git commit -m "chore(pi): remove Health Score bar from ScanResult UI"
```

---

### Task 5: Pi — Update tests/test_database.py

**Files:**
- Modify: `pi-app/tests/test_database.py`

- [ ] **Step 1: Update the file**

Replace the entire file with:

```python
import sqlite3
import pytest
from database.db import get_connection, run_migrations
from database.seed_foods import seed_foods

def test_migrations_create_tables():
    conn = get_connection(":memory:")
    run_migrations(conn)
    tables = {r[0] for r in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table'"
    ).fetchall()}
    assert "foods" in tables
    assert "users" in tables
    assert "food_log" in tables

def test_foods_table_columns():
    conn = get_connection(":memory:")
    run_migrations(conn)
    cols = {r[1] for r in conn.execute("PRAGMA table_info(foods)").fetchall()}
    assert cols >= {
        "id", "name", "calories_per_100g", "protein_per_100g",
        "fat_per_100g", "sugar_per_100g", "fiber_per_100g",
        "is_healthy", "huskylens_label_id"
    }
    assert "health_score" not in cols

def test_users_table_columns():
    conn = get_connection(":memory:")
    run_migrations(conn)
    cols = {r[1] for r in conn.execute("PRAGMA table_info(users)").fetchall()}
    assert cols >= {
        "id", "bluetooth_mac", "name", "age",
        "weight_kg", "height_cm", "sex", "activity_level", "daily_calorie_goal"
    }

def test_food_log_foreign_keys():
    conn = get_connection(":memory:")
    conn.execute("PRAGMA foreign_keys = ON")
    run_migrations(conn)
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute(
            "INSERT INTO food_log (user_id, food_id, weight_g, calories) VALUES (999, 999, 100, 50)"
        )
        conn.commit()

def test_seed_inserts_foods(seeded_db):
    count = seeded_db.execute("SELECT COUNT(*) FROM foods").fetchone()[0]
    assert count == 25
```

- [ ] **Step 2: Run the Pi tests**

```bash
cd pi-app && python -m pytest tests/test_database.py -v
```

Expected: all 5 tests pass.

- [ ] **Step 3: Commit**

```bash
git add pi-app/tests/test_database.py
git commit -m "test(pi): update column assertions, remove health_score tests"
```

---

### Task 6: Android — Update FoodLogEntry.kt

**Files:**
- Modify: `android-app/app/src/main/java/com/fibonacci/fibohealth/data/model/FoodLogEntry.kt`

- [ ] **Step 1: Remove `healthScore` field and deserializer line**

Replace the entire file with:

```kotlin
package com.fibonacci.fibohealth.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable(with = FoodLogEntrySerializer::class)
data class FoodLogEntry(
    val id: Int,
    val foodName: String,
    val weightG: Float,
    val calories: Float,
    val isHealthy: Boolean,
    val timestamp: String,
    val proteinG: Float? = null,
    val fatG: Float? = null,
    val sugarG: Float? = null,
    val fiberG: Float? = null
)

// Custom serializer handles is_healthy as Int or Boolean
object FoodLogEntrySerializer : kotlinx.serialization.KSerializer<FoodLogEntry> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("FoodLogEntry")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: FoodLogEntry) {
        // Write-path not needed (Pi → phone only)
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): FoodLogEntry {
        val obj = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val isHealthy = obj["is_healthy"]?.let {
            when {
                it is JsonPrimitive && it.isString -> it.content == "true"
                it is JsonPrimitive -> it.intOrNull?.let { n -> n != 0 } ?: it.booleanOrNull ?: false
                else -> false
            }
        } ?: false
        return FoodLogEntry(
            id        = obj["id"]!!.jsonPrimitive.int,
            foodName  = obj["food_name"]!!.jsonPrimitive.content,
            weightG   = obj["weight_g"]!!.jsonPrimitive.float,
            calories  = obj["calories"]!!.jsonPrimitive.float,
            isHealthy = isHealthy,
            timestamp = obj["timestamp"]!!.jsonPrimitive.content,
            proteinG  = obj["protein_g"]?.jsonPrimitive?.floatOrNull,
            fatG      = obj["fat_g"]?.jsonPrimitive?.floatOrNull,
            sugarG    = obj["sugar_g"]?.jsonPrimitive?.floatOrNull,
            fiberG    = obj["fiber_g"]?.jsonPrimitive?.floatOrNull
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android-app/app/src/main/java/com/fibonacci/fibohealth/data/model/FoodLogEntry.kt
git commit -m "chore(android): remove healthScore from FoodLogEntry model"
```

---

### Task 7: Android — Update FoodLogEntryDecoderTest.kt

**Files:**
- Modify: `android-app/app/src/test/java/com/fibonacci/fibohealth/FoodLogEntryDecoderTest.kt`

- [ ] **Step 1: Remove `health_score` from test JSON strings**

Replace the entire file with:

```kotlin
package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.data.model.FoodLogEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodLogEntryDecoderTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun `decodes full payload with macros`() {
        val raw = """{"id":1,"food_name":"Apple","weight_g":150.0,"calories":52.0,
            "is_healthy":true,"timestamp":"2026-04-16T08:30:00Z",
            "protein_g":0.3,"fat_g":0.2,"sugar_g":10.4,"fiber_g":2.1}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Apple", entry.foodName)
        assertEquals(0.3f, entry.proteinG ?: 0f, 0.01f)
    }

    @Test fun `decodes legacy payload missing macros`() {
        val raw = """{"id":2,"food_name":"Salad","weight_g":300.0,"calories":90.0,
            "is_healthy":1,"timestamp":"2026-04-16T12:00:00Z"}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Salad", entry.foodName)
        assertNull(entry.proteinG)
    }
}
```

- [ ] **Step 2: Run Android unit tests**

```bash
cd android-app && ./gradlew :app:testDebugUnitTest --tests "com.fibonacci.fibohealth.FoodLogEntryDecoderTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with 2 tests passing.

- [ ] **Step 3: Commit**

```bash
git add android-app/app/src/test/java/com/fibonacci/fibohealth/FoodLogEntryDecoderTest.kt
git commit -m "test(android): remove health_score from decoder test JSON"
```

---

### Task 8: iOS — Update FoodLogEntry.swift

**Files:**
- Modify: `ios-app/FiboHealth/Models/FoodLogEntry.swift`

- [ ] **Step 1: Remove `healthScore` property, coding key, init parameter, and decode line**

Replace the entire file with:

```swift
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
```

- [ ] **Step 2: Commit**

```bash
git add ios-app/FiboHealth/Models/FoodLogEntry.swift
git commit -m "chore(ios): remove healthScore from FoodLogEntry model"
```

---

### Task 9: iOS — Update FoodLogEntryDecodeTests.swift

**Files:**
- Modify: `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift`

- [ ] **Step 1: Remove `health_score` from test JSON**

Replace the entire file with:

```swift
import XCTest
@testable import FiboHealth

final class FoodLogEntryDecodeTests: XCTestCase {
    func test_decodesNewMacroFields() throws {
        let json = """
        {
          "id": 7, "food_name": "Chicken Breast", "weight_g": 200,
          "calories": 330, "is_healthy": 1,
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
          "calories": 52, "is_healthy": 1,
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

- [ ] **Step 2: Run iOS tests**

Open Xcode or run via command line:

```bash
cd ios-app && xcodebuild test -scheme FiboHealth -destination 'platform=iOS Simulator,name=iPhone 16' 2>&1 | grep -E "Test (Suite|Case|passed|failed)|error:"
```

Expected: `FoodLogEntryDecodeTests` — 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift
git commit -m "test(ios): remove health_score from FoodLogEntry decode tests"
```
