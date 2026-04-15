# HealthKit Food Logging — Design

**Date:** 2026-04-15
**Status:** Approved (pending user review of this written spec)

## Goal

Log food the user scans on the Pi to Apple HealthKit on iOS, including nutrient
breakdown, with the Pi remaining the source of truth: deletions on the Pi
cascade to HealthKit. The feature is gated by a user-facing toggle in the
**system Settings app** under FiboHealth, defaulting to **OFF**.

## User-facing surface

### System Settings (`Settings.bundle`)

A new bundle resource shipped in the iOS app:

- **Group "Apple Health"**, footer text:
  > "When on, food scans from your Pi are logged to Health as Dietary Energy
  > and macronutrients. To remove previously logged entries, open
  > FiboHealth → Profile."
- **Toggle "Log Food to Health"**, key `healthkit_food_logging_enabled`,
  default `NO`.

### In-app — `ProfileView` "Health Integration" section (new, bottom of form)

- **Status row.** Title "Log Food to Health". Trailing icon reflects state:
  `checkmark.circle.fill` (green) when toggle ON *and* HK write auth granted;
  `xmark.circle` (muted) otherwise. Caption: *"Manage in Settings → FiboHealth."*
- **Destructive button:** `Button(role: .destructive)` labelled
  **"Remove FiboHealth Food Entries from Health"**. Tap → confirmation alert.

### Destructive confirmation alert (HIG-compliant)

- **Title:** "Remove Food Entries from Health?"
- **Message:** "This will permanently delete all food entries FiboHealth has
  written to Apple Health. Activity data and entries from other apps are not
  affected. This cannot be undone."
- **Buttons:** `Cancel` (`.cancel`) and `Remove` (`.destructive` — SwiftUI
  renders red automatically per HIG).
- After completion, a non-destructive follow-up alert reports the result:
  *"Removed N entries."* (or *"Removed N entries, M failed."* on partial
  failure).

`FoodLogView` is unchanged.

## Architecture

### New iOS service: `HealthKitFoodLogger`

Sibling to the existing `HealthKitService`. Single responsibility: reconcile
the current Pi food log with HealthKit food correlations written by this app.

Public surface:

```swift
final class HealthKitFoodLogger: ObservableObject {
    @Published var lastError: String?
    func requestWriteAuthorization() async
    func reconcile(with entries: [FoodLogEntry]) async
    func removeAllFiboHealthEntries() async -> (removed: Int, failed: Int)
}
```

Internal helper, exposed for unit tests:

```swift
static func diff(piIds: Set<Int>, hkIds: Set<Int>)
    -> (toInsert: Set<Int>, toDelete: Set<Int>)
```

### Wiring in `AppEnvironment`

- Owns a `HealthKitFoodLogger` instance.
- Subscribes to `bluetooth.$foodLog` — on each emission, if the toggle is ON,
  calls `logger.reconcile(with: entries)`.
- Subscribes to `UserDefaults.didChangeNotification` — on toggle flip ON,
  re-requests HK write auth then reconciles with the current `bluetooth.foodLog`.
  On flip OFF, does nothing destructive.

All reconcile call sites are guarded by:
```swift
UserDefaults.standard.bool(forKey: "healthkit_food_logging_enabled")
```

## Data model

### Pi → iOS payload extension

The `food_log` rows currently shipped over the
`CHAR_FOOD_LOG_SYNC` characteristic carry only `weight_g`, `calories`,
`is_healthy`, `health_score`. The Pi's serialization (in `bluetooth_server.py`
and any helper that builds the food-log JSON) must JOIN the `foods` table and
add per-entry grams:

```python
protein_g = protein_per_100g * weight_g / 100.0
fat_g     = fat_per_100g     * weight_g / 100.0
sugar_g   = sugar_per_100g   * weight_g / 100.0
fiber_g   = fiber_per_100g   * weight_g / 100.0
```

These four fields are added to each entry as JSON keys `protein_g`, `fat_g`,
`sugar_g`, `fiber_g`. **Total carbohydrates are intentionally not written** —
the schema lacks a total-carbs column and `sugar + fiber` would under-report.

### iOS `FoodLogEntry` extension

Add four optional fields with permissive decoding (older payloads without
these keys must still decode, mirroring the existing `is_healthy` permissive
decode):

```swift
var proteinG: Double?
var fatG: Double?
var sugarG: Double?
var fiberG: Double?
```

CodingKeys: `protein_g`, `fat_g`, `sugar_g`, `fiber_g`.

### HealthKit sample shape

For each Pi entry, write **one `HKCorrelation` of type
`HKCorrelationType.correlationType(forIdentifier: .food)`** containing one
`HKQuantitySample` per available nutrient:

| Pi field    | HK identifier              | Unit              |
|-------------|----------------------------|-------------------|
| `calories`  | `dietaryEnergyConsumed`    | `.kilocalorie()`  |
| `protein_g` | `dietaryProtein`           | `.gram()`         |
| `fat_g`     | `dietaryFatTotal`          | `.gram()`         |
| `sugar_g`   | `dietarySugar`             | `.gram()`         |
| `fiber_g`   | `dietaryFiber`             | `.gram()`         |

The correlation and each member sample carry metadata:

```
"pi_food_id":  "<Int as String>"
"source_app":  "FiboHealth"
HKMetadataKeyFoodType: <foodName>
```

Both the correlation `start`/`end` and each child sample's `start`/`end` are
the parsed `timestamp`. Deleting the correlation deletes its member samples
in HealthKit, so the cascade-on-delete logic operates at the correlation
level only.

If a macro field is **missing** (older Pi payload, key absent), that one
quantity sample is omitted. A macro that is explicitly **0** is still
written (e.g. "0 g fiber" on chicken is correct, useful data). Calories are
always required — entries without a parseable kcal value are skipped
entirely.

## Reconciliation algorithm

`reconcile(with entries: [FoodLogEntry])`:

1. Query HK for all correlations of type `food` with metadata predicate
   `source_app == "FiboHealth"`, using
   `HKQuery.predicateForObjects(withMetadataKey:operatorType:value:)`.
2. Build `hkIds: Set<Int>` from each correlation's `pi_food_id` metadata.
3. Build `piIds: Set<Int>` from `entries`.
4. `(toInsert, toDelete) = diff(piIds:, hkIds:)`.
5. For each id in `toInsert`: build the correlation as above and
   `store.save(correlation)`.
6. For each id in `toDelete`: `store.delete(correlation)` for the matching
   correlation found in step 1.

HealthKit is the single source of truth for the mapping — no `UserDefaults`
or SwiftData cache. This avoids drift; the only cost is one extra HK query
per sync (cheap, scoped by metadata predicate).

## Authorization & plist changes

### `Info.plist`

Replace existing `NSHealthUpdateUsageDescription`
(currently *"FiboHealth does not write health data."*) with:

> "FiboHealth logs the food you scan on your Pi as dietary energy and
> macronutrients in Apple Health, so your daily calorie balance stays
> accurate."

`NSHealthShareUsageDescription` is unchanged.

### `HealthKitService.requestAuthorization`

Extend the share set (currently `[]`) to include the five quantity types
listed in the table above. Read set unchanged.

`requestWriteAuthorization()` on the new `HealthKitFoodLogger` requests the
same share set (callable independently when the user flips the toggle ON
later).

### Entitlements

`FiboHealth.entitlements` already declares `com.apple.developer.healthkit`
with an empty `healthkit.access` array — no change needed.
(`healthkit.access` gates *clinical records* access, not nutrition.)

## Timestamp parsing

The Pi's `food_log.timestamp` default is
`strftime('%Y-%m-%dT%H:%M:%S', 'now')`. The logger holds a single
`ISO8601DateFormatter` configured for `[.withFullDate, .withTime,
.withColonSeparatorInTime]` (no fractional seconds, no timezone — Pi stores
local naive time and we treat it as such for now).

If parsing fails, the entry is **skipped, not logged at `Date()`** — better
to lose one row than pollute Today's HealthKit totals with a mis-dated
sample.

## Error handling

- **HK auth denied while toggle ON:** `lastError` set on the logger;
  `ProfileView` shows the message muted under the status row. No alerts on
  background syncs.
- **Timestamp unparseable:** entry skipped; one `os_log` warning per skip.
- **Save failure:** logged; reconciliation continues with remaining ids.
- **Delete failure:** counted; surfaced in the post-removal alert
  (*"Removed N entries, M failed."*).

## Testing plan

### Unit-testable

- `HealthKitFoodLogger.diff(piIds:hkIds:)` — pure function over `Set<Int>`.
  Test cases: empty/empty, all-insert, all-delete, mixed.
- `FoodLogEntry` decoding — payloads with and without the new macro keys
  both decode.

### Manual on-device (HealthKit Simulator support is limited)

1. Fresh install → toggle OFF → scan food on Pi → confirm **nothing** in
   Health → Browse → Nutrition.
2. Flip toggle ON in Settings → re-scan → entry appears in Health under
   Dietary Energy *and* Protein/Fat/Sugar/Fiber rows.
3. Delete the entry on the Pi (existing UI) → entry disappears from Health
   within one sync cycle.
4. Flip toggle OFF → previous entries remain visible in Health.
5. Profile → "Remove FiboHealth Food Entries from Health" → confirm red
   destructive alert → confirm Health rows now gone.
6. Re-flip toggle ON → next Pi sync re-inserts current entries (because
   `hkIds` is empty after step 5).

## Out of scope

- **Total carbohydrates** — schema gap; would require a Pi data-model change.
- **Background BLE-driven HK writes when app is suspended** — current BLE
  flow only syncs while the app is connected; HK writes inherit that.
- **Migrating historically-written samples** — the feature is new, no prior
  samples exist.
- **Per-meal grouping** (breakfast/lunch/dinner metadata) — Pi doesn't track
  this.
