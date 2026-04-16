# FiboHealth Android Port — Design Spec

**Date:** 2026-04-16
**Status:** Approved
**Scope:** Android port of the iOS FiboHealth app. The iOS app is not modified.

---

## Overview

FiboHealth Android is a native Android companion app for the Fibonacci Pi food scale. It is a feature-equivalent port of the iOS SwiftUI app, adapted to Android conventions — Material 3 UI, Health Connect instead of HealthKit, and a BLE Foreground Service instead of CoreBluetooth background mode.

The Pi app (Raspberry Pi) is unchanged. The same BLE GATT protocol, UUIDs, and chunking format are used.

The Android app lives in a new top-level `android-app/` directory alongside the existing `ios-app/` and `pi-app/` directories.

---

## Decisions

| Question | Decision | Reason |
|---|---|---|
| Tech stack | Native Kotlin + Jetpack Compose | Best Android feel, first-class Material 3, full API access |
| Minimum Android version | Android 14 (API 34) | Health Connect is built-in, no separate install required |
| Navigation on tablet | Adaptive rail + two-pane | Material 3 recommended pattern, `NavigationSuiteScaffold` handles it automatically |
| BLE background | Foreground Service with persistent notification | Matches iOS behavior; Android requires this for background BLE |
| Architecture | MVVM + Hilt + Room | Idiomatic Android, well-tested at scale, clean lifecycle handling |

---

## Project Structure

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/fibonacci/fibohealth/
│   │   │   ├── FiboHealthApp.kt              # Application class (Hilt entry point)
│   │   │   ├── MainActivity.kt               # Single-activity host
│   │   │   ├── ui/
│   │   │   │   ├── navigation/               # NavHost + adaptive scaffold
│   │   │   │   ├── dashboard/                # DashboardScreen + DashboardViewModel
│   │   │   │   ├── foodlog/                  # FoodLogScreen + FoodLogViewModel
│   │   │   │   ├── activity/                 # ActivityScreen + ActivityViewModel
│   │   │   │   ├── profile/                  # ProfileScreen + ProfileViewModel
│   │   │   │   ├── device/                   # DeviceScreen + DeviceViewModel
│   │   │   │   ├── components/               # CalorieRing, StatCard, HealthBadge, MacroBar
│   │   │   │   └── theme/                    # Material 3 color scheme + typography
│   │   │   ├── data/
│   │   │   │   ├── model/                    # UserProfile, FoodLogEntry, HealthSnapshot, SessionState
│   │   │   │   ├── local/                    # Room database + DAOs
│   │   │   │   └── repository/               # ProfileRepository
│   │   │   └── service/
│   │   │       ├── BleService.kt             # Foreground Service (owns BleClient)
│   │   │       ├── BleClient.kt              # GATT client + chunked reassembly
│   │   │       ├── HealthConnectService.kt   # Reads activity data
│   │   │       └── HealthConnectFoodLogger.kt # Reconcile Pi food log → Health Connect
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## Architecture

**Pattern:** MVVM with Hilt dependency injection.

- `FiboHealthApp` is the Hilt `@HiltAndroidApp` application class. It initialises the DI graph.
- `MainActivity` is the single activity. It hosts the Compose `NavHost` wrapped in a `NavigationSuiteScaffold`.
- Each screen has its own `ViewModel` that collects `StateFlow`s from injected services.
- Services (`BleClient`, `HealthConnectService`, `HealthConnectFoodLogger`) are Hilt `@Singleton`s exposed to ViewModels via constructor injection.
- `BleService` is a started `ForegroundService` that owns `BleClient`. ViewModels bind to it via a service connection and collect its exposed `SharedFlow<BleEvent>`.
- `ProfileRepository` wraps the Room `UserProfileDao` and exposes a `Flow<UserProfile>`.

**Key dependencies:**
- `androidx.compose.*` + `material3` — UI
- `com.google.dagger:hilt-android` — DI
- `androidx.room:room-runtime` — local persistence
- `androidx.health.connect:connect-client` — Health Connect SDK
- Android BLE APIs (no third-party library; same approach as iOS CoreBluetooth usage)
- `androidx.navigation:navigation-compose` — in-app navigation
- `androidx.window:window` — window size classes for adaptive layout

---

## Navigation & Adaptive Layout

`NavigationSuiteScaffold` (from `androidx.compose.material3.adaptive:navigation-suite`) automatically selects:

- **Phone (< 600dp width):** Bottom navigation bar with 5 tabs
- **Tablet (≥ 600dp width):** Navigation rail on the left side

**5 destinations** (identical to iOS):
1. Home (Dashboard)
2. Log (Food Log)
3. Activity
4. Profile
5. Device

**Two-pane on tablet:** The Food Log screen uses `ListDetailPaneScaffold`. Tapping an entry shows the detail panel on the right. On phone, tapping navigates to a full detail screen pushed onto the back stack.

**Icons:** Material Icons Round — the Android semantic equivalent of SF Symbols. One icon per tab (home, restaurant_menu, bolt, person, bluetooth).

---

## BLE Service

`BleService` is a `ForegroundService` with type `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.

**Notification states:**
- Scanning: "FiboHealth · Scanning for Pi..."
- Connected: "FiboHealth Pi · Connected" + "Disconnect" action button

**BleClient** implements the same GATT client logic as iOS `BluetoothClient.swift`:
- Same service UUID: `12345678-1234-5678-1234-56789abcdef0`
- Same 4 characteristic UUIDs (`def1`–`def4`)
- Same chunking protocol: `0xFB` magic, `seq`, `total`, payload slice; chunk size 150 bytes
- Reassembles chunks before JSON decoding
- On connect: writes user profile + health snapshot characteristics
- Subscribes to food log sync + session state notifications
- Auto-reconnects on disconnect by restarting scan

**Exposed to ViewModels:**
- `isConnected: StateFlow<Boolean>`
- `isScanning: StateFlow<Boolean>`
- `foodLog: StateFlow<List<FoodLogEntry>>`
- `sessionState: StateFlow<SessionState?>`
- `lastSyncTime: StateFlow<Instant?>`

---

## Health Connect

**Minimum version:** Health Connect built into Android 14 (API 34). No separate Play Store install required.

**Permissions declared in `AndroidManifest.xml`:**
```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
<uses-permission android:name="android.permission.health.READ_BASAL_METABOLIC_RATE" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
<uses-permission android:name="android.permission.health.WRITE_NUTRITION" />
```

**HealthConnectService** reads today's activity data:

| iOS HealthKit | Android Health Connect record |
|---|---|
| Steps | `StepsRecord` |
| Active energy | `ActiveCaloriesBurnedRecord` |
| Basal energy | `BasalMetabolicRateRecord` |
| Exercise time | `ExerciseSessionRecord` (duration sum) |
| Workouts | `ExerciseSessionRecord` (count) |

**HealthConnectFoodLogger** mirrors iOS `HealthKitFoodLogger`:
- Reconciles Pi food log against existing `NutritionRecord`s in Health Connect
- Identifies records by `pi_food_id` stored in record metadata
- Inserts new entries, deletes removed entries, prunes duplicates (keep first, delete extras)
- Single-flight serialisation via Kotlin `Mutex` (mirrors iOS actor/serial queue)
- Writes one `NutritionRecord` per food entry with: `energy`, `protein`, `totalFat`, `sugar`, `dietaryFiber`

**Permission re-check:** On each `Activity.onResume`, the app checks Health Connect permission status. If revoked, a non-dismissible banner is shown on the Activity screen with a "Grant Access" button that re-launches the HC permission flow.

---

## Data Layer

### Models

Identical fields to iOS counterparts:

- `UserProfile` — id, deviceId, name, age, weightKg, heightCm, sex, activityLevel, dailyCalorieGoal. `blePayload()` serialises to JSON for Pi. `calculatedDailyGoal` uses Mifflin-St Jeor + activity multiplier.
- `FoodLogEntry` — id, foodName, weightG, calories, isHealthy, healthScore, timestamp, optional macros. Deserialised from BLE JSON (tolerates missing macro fields and `is_healthy` as Int or Boolean).
- `HealthSnapshot` — date, steps, caloriesBurned, activeMinutes, workouts.
- `SessionState` — caloriesConsumed, caloriesRemaining, lastScanFood, lastScanKcal.

### Persistence

- **Room** — single `user_profile` table. `ProfileRepository` wraps the DAO and exposes `Flow<UserProfile>`.
- `FoodLogEntry` is not persisted locally — sourced live from the BLE `StateFlow`.
- **DataStore (Preferences)** — stores the "Log Food to Health Connect" boolean toggle. Replaces iOS `Settings.bundle` / `UserDefaults`. The toggle lives in the Profile screen (a Compose `Switch`), not the Android Settings app.

---

## Theme & Design Tokens

The same visual identity as iOS. Colors are defined as a custom Material 3 `ColorScheme`.

| iOS token | Material 3 slot |
|---|---|
| `primaryBg` dark `#0f172a` | `colorScheme.background` (dark) |
| `surface` dark `#1e293b` | `colorScheme.surface` (dark) |
| `primaryBg` light `#f8fafc` | `colorScheme.background` (light) |
| `surface` light `#ffffff` | `colorScheme.surface` (light) |
| `accentIndigo` `#6366f1` | `colorScheme.primary` |
| `accentCyan` `#06b6d4` | `colorScheme.secondary` |
| `textPrimary` | `colorScheme.onBackground` |
| `textSecondary` | `colorScheme.onSurfaceVariant` |

Status colors (`#22c55e` green, `#f59e0b` amber, `#ef4444` red) are defined as plain `Color` constants outside the Material scheme — they carry fixed semantic meaning and must not adapt to system themes.

Dark/light mode follows the system setting via `isSystemInDarkTheme()` in the top-level `FiboHealthTheme` composable.

---

## Reusable Components

Direct ports of iOS components to Compose:

| iOS | Android Composable | Implementation |
|---|---|---|
| `CalorieRingView` | `CalorieRing` | `Canvas` + `drawArc` with animated sweep angle |
| `StatCardView` | `StatCard` | `Card` composable with label + colored value |
| `HealthBadgeView` | `HealthBadge` | `SuggestionChip` with check/x icon |
| `MacroBarView` | `MacroBar` | `LinearProgressIndicator` per macro |

---

## Permissions (AndroidManifest.xml)

```xml
<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- Health Connect (declared + queried via HealthConnectClient) -->
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
<uses-permission android:name="android.permission.health.READ_BASAL_METABOLIC_RATE" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
<uses-permission android:name="android.permission.health.WRITE_NUTRITION" />
```

The Health Connect privacy policy intent filter is declared on `MainActivity` as required by the SDK.

---

## Testing

Mirrors iOS test coverage. No UI tests in scope.

| Test | What it covers |
|---|---|
| BLE chunked payload assembly | Reassembler handles split frames, out-of-order seq bytes, partial chunks |
| Food log diff | Insert new Pi entries, delete removed entries, prune duplicate HC records |
| `FoodLogEntry` JSON decoding | Tolerates missing macro fields; `is_healthy` as Int or Boolean |
| Mifflin-St Jeor BMR | Correct TDEE for known inputs across sex/activity combinations |

---

## What Is Not Changing

- The iOS app — zero modifications.
- The Pi app — zero modifications.
- The BLE GATT protocol, UUIDs, and chunking format — identical wire format.
- The food reconcile diff algorithm — same logic, different platform API.
