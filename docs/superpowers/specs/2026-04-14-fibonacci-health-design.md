# Fibonacci Health — Design Spec
*Fibonacci Robotics Olympiad 2026 · April 14, 2026*

---

## Overview

A food-scale smart nutrition system for the Fibonacci Robotics Olympiad. A Raspberry Pi with a HuskyLens K210 camera identifies food placed on a loadcell scale via UART, looks it up in a local nutrition database, and calculates calories from the measured weight. A companion iOS app syncs the user's health data (HealthKit + manual profile) to the Pi over Bluetooth, enabling the Pi to show remaining daily calories and activity stats. All processing is fully offline — no internet connection required.

---

## Hardware

| Component | Interface | Notes |
|---|---|---|
| Raspberry Pi (4 or 5) | — | Central compute |
| HuskyLens K210 | UART (Serial) | Food object detection, sends numeric label IDs |
| HX711 + Loadcell | GPIO (2-wire: DOUT + SCK) | Weight scale, grams |
| LCD Touchscreen 5" 720×1280 | DSI / HDMI | Portrait orientation, capacitive touch |

---

## System Architecture

```
HuskyLens K210 ──UART──► FoodDetectionService ──►┐
HX711 Loadcell ──GPIO──► WeightService          ──►  CalorieCalculator ──► SQLite
                                                  │                           │
iOS App ──BLE GATT──► BluetoothServer           ──►  UserSessionManager ◄────┘
                                                              │
                                                         QML UI (PyQt6)
```

**Pi is always the BLE peripheral (server).** The iOS app is the BLE central (client). On connect, the phone pushes profile + health data; the Pi responds with the food log and session state.

---

## Raspberry Pi App

### Technology Stack
- **Language:** Python 3.11+
- **UI Framework:** PyQt6 with QML for all screens
- **Database:** SQLite 3 (via Python `sqlite3`)
- **BLE:** BlueZ + `dbus-python` (GATT peripheral)
- **HuskyLens UART:** `pyserial`
- **HX711:** `hx711` Python library

### Services

**FoodDetectionService**
- Listens on UART for HuskyLens label IDs
- On each detection: queries `foods` table by `huskylens_label_id`
- Emits a Qt signal with the matched `Food` object (or `None` if unmapped)
- In Test Mode: bypasses UART, exposes a method to inject a food ID manually

**WeightService**
- Polls HX711 at ~10 Hz, applies tare and calibration factor
- Exposes current stable weight as a Qt property. Stability = 5 consecutive readings within ±2g of each other; otherwise reports 0
- In Test Mode: returns a manually set weight value

**CalorieCalculator**
- `calculate(food, weight_g) → calories`  using `food.calories_per_100g * weight_g / 100`
- `remaining(user, consumed) → float` using `user.daily_calorie_goal - consumed`
- Daily goal = manual override if set, else Mifflin-St Jeor BMR × activity multiplier:
  - Sedentary ×1.2, Light ×1.375, Moderate ×1.55, Active ×1.725
  - BMR (male): 10×weight_kg + 6.25×height_cm − 5×age + 5
  - BMR (female): 10×weight_kg + 6.25×height_cm − 5×age − 161
  - BMR (other): average of male/female formulas

**BluetoothServer**
- Registers a custom GATT service `FiboHealth` with four characteristics (see BLE Protocol)
- Identifies connecting device by BT MAC address
- On connect: creates or loads user profile from `users` table
- Triggers sync: sends `FoodLogSync` + `SessionState` notifications
- When no device is connected: Dashboard shows a "Connect your phone" prompt; scanning still works but scans are stored under a transient guest session (MAC = `00:00:00:00:00:00`) until a real user connects

**UserSessionManager**
- Maintains active user state (current user, today's food log, consumed calories)
- Multi-user: keyed by Bluetooth MAC address
- Persists all scans to `food_log` table

### QML Screens

| Screen | Purpose |
|---|---|
| **Dashboard** | Calorie ring, consumed/goal/burned, last scan summary, activity strip, BLE status |
| **Scan Result** | Food name, weight, calorie total, macro bars, health score, healthy/unhealthy badge, remaining-after-this, Add to Log / Discard |
| **Food Log** | Scrollable list of today's scans per user, total calories, delete entry |
| **Activity** | Steps, active minutes, calories burned (synced from iOS HealthKit) |
| **Settings** | Test Mode toggle + manual food picker + weight input, dark/light theme toggle, HuskyLens label mapping table, paired users list |

### Test Mode
- Toggled from the Settings screen
- Replaces `FoodDetectionService` UART input with a dropdown picker of all foods in the DB
- Replaces `WeightService` GPIO reading with a numeric text input
- A visible amber banner `TEST MODE` is shown on all screens when active
- All other logic (calorie calculation, BLE sync, database writes) runs identically

---

## iOS App

### Technology Stack
- **Language:** Swift 5.9+
- **UI Framework:** SwiftUI
- **Health data:** HealthKit
- **Bluetooth:** CoreBluetooth (BLE central)
- **Persistence:** SwiftData (local, no cloud)

### Screens

| Screen | Purpose |
|---|---|
| **Dashboard** | Calorie ring, consumed/goal/burned, HealthKit activity strip, recent scans from Pi |
| **Food Log** | Full history of Pi-synced scans, grouped by day, with calorie totals |
| **Activity** | Steps, active minutes, workouts, calories burned — sourced from HealthKit |
| **Profile** | Name, age, weight, height, sex, activity level, daily calorie goal (manual override). Changes sync to Pi on next BLE connect. |
| **Device** | Pi pairing, connection status, last sync time, disconnect |

### Services

**HealthKitService**
- Requests permission for: `stepCount`, `activeEnergyBurned`, `basalEnergyBurned`, `appleExerciseTime`, `workoutType`
- Aggregates today's totals and packages into `HealthSnapshot`
- Falls back gracefully if permissions denied (shows manual-only mode)

**BluetoothClient**
- Scans for Pi advertising `FiboHealth` GATT service UUID
- On connect: writes `UserProfile` + `HealthSnapshot` characteristics
- Subscribes to `FoodLogSync` + `SessionState` notifications
- Auto-reconnects when Pi is in range

**UserProfileStore** (SwiftData)
- Stores user profile locally; sends to Pi on every connect
- Multiple profiles supported (one per Pi MAC if user has multiple devices)

---

## Data Model (SQLite on Pi)

### `foods`
```sql
CREATE TABLE foods (
    id                  INTEGER PRIMARY KEY,
    name                TEXT NOT NULL,
    calories_per_100g   REAL NOT NULL,
    protein_per_100g    REAL,
    fat_per_100g        REAL,
    sugar_per_100g      REAL,
    fiber_per_100g      REAL,
    is_healthy          BOOLEAN DEFAULT 0,
    health_score        REAL,        -- 0–100, computed on insert/update
    huskylens_label_id  INTEGER UNIQUE  -- NULL if not yet mapped
);
```

**health_score formula** (0–100, computed from `foods` columns):
- Base 50
- +10 per g `fiber_per_100g` (up to +20)
- +5 per g `protein_per_100g` (up to +20)
- −8 per g `sugar_per_100g` (down to −20)
- −10 per g `fat_per_100g` (down to −20)
- Clamped to [0, 100], rounded to 1 decimal
- Recomputed and stored whenever a food row is inserted or updated

### `users`
```sql
CREATE TABLE users (
    id                  INTEGER PRIMARY KEY,
    bluetooth_mac       TEXT UNIQUE NOT NULL,
    name                TEXT,
    age                 INTEGER,
    weight_kg           REAL,
    height_cm           REAL,
    sex                 TEXT,           -- 'male' | 'female' | 'other'
    activity_level      TEXT,           -- 'sedentary' | 'light' | 'moderate' | 'active'
    daily_calorie_goal  REAL            -- NULL = use calculated BMR
);
```

### `food_log`
```sql
CREATE TABLE food_log (
    id          INTEGER PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id),
    food_id     INTEGER NOT NULL REFERENCES foods(id),
    weight_g    REAL NOT NULL,
    calories    REAL NOT NULL,
    timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## BLE GATT Protocol

**Service UUID:** `FiboHealth` (custom 128-bit UUID)

| Characteristic | Direction | Format | Trigger |
|---|---|---|---|
| `UserProfile` | iOS → Pi (Write) | JSON: `{name, age, weight_kg, height_cm, sex, activity_level, daily_calorie_goal}` | On every connect |
| `HealthSnapshot` | iOS → Pi (Write) | JSON: `{date, steps, calories_burned, active_minutes, workouts}` | On every connect |
| `FoodLogSync` | Pi → iOS (Notify) | JSON array of today's `food_log` rows with food names | After write received |
| `SessionState` | Pi → iOS (Notify) | JSON: `{calories_consumed, calories_remaining, last_scan_food, last_scan_kcal}` | After write received + after each scan |

Auto-sync flow on connect:
1. iOS writes `UserProfile` → Pi upserts user row
2. iOS writes `HealthSnapshot` → Pi stores for display
3. Pi notifies `FoodLogSync` + `SessionState` → iOS updates UI

---

## Visual Design

### Theme
Two switchable themes sharing the same design tokens:

| Token | Dark Cosmos | Clean Light |
|---|---|---|
| Background | `#0f172a` | `#f8fafc` |
| Surface | `#1e293b` | `#ffffff` |
| Border | `#334155` | `#e2e8f0` |
| Primary gradient | `#6366f1 → #06b6d4` | `#6366f1 → #06b6d4` |
| Text primary | `#ffffff` | `#0f172a` |
| Text muted | `#64748b` | `#94a3b8` |
| Healthy | `#34d399` / `#064e3b` bg | `#065f46` / `#d1fae5` bg |
| Unhealthy | `#f87171` / `#450a0a` bg | `#991b1b` / `#fee2e2` bg |
| Calories burned | `#f59e0b` | `#f59e0b` |

Theme toggle is available on the Pi Settings screen and in the iOS Profile screen. Selection persists locally on each device.

### Design Principles
- Same component vocabulary on both Pi (QML) and iOS (SwiftUI): calorie ring, macro bars, health badge, stat cards
- Portrait layout on both — Pi 720×1280, iOS standard portrait
- Bottom navigation bar on both (4 tabs)
- Indigo-to-cyan gradient used only for primary actions and the calorie ring fill

---

## File / Project Structure

```
Fibonacci 2026/
├── pi-app/
│   ├── main.py
│   ├── services/
│   │   ├── food_detection.py
│   │   ├── weight.py
│   │   ├── calorie_calculator.py
│   │   ├── bluetooth_server.py
│   │   └── user_session.py
│   ├── database/
│   │   ├── db.py              # connection + migrations
│   │   ├── schema.sql
│   │   └── seed_foods.py      # pre-populated food data
│   ├── ui/
│   │   ├── main.qml
│   │   ├── screens/
│   │   │   ├── Dashboard.qml
│   │   │   ├── ScanResult.qml
│   │   │   ├── FoodLog.qml
│   │   │   ├── Activity.qml
│   │   │   └── Settings.qml
│   │   └── components/
│   │       ├── CalorieRing.qml
│   │       ├── MacroBar.qml
│   │       ├── HealthBadge.qml
│   │       └── StatCard.qml
│   └── requirements.txt
│
├── ios-app/
│   └── FiboHealth/
│       ├── App/
│       ├── Services/
│       │   ├── HealthKitService.swift
│       │   ├── BluetoothClient.swift
│       │   └── UserProfileStore.swift
│       ├── Views/
│       │   ├── DashboardView.swift
│       │   ├── FoodLogView.swift
│       │   ├── ActivityView.swift
│       │   ├── ProfileView.swift
│       │   └── DeviceView.swift
│       └── Components/
│           ├── CalorieRingView.swift
│           ├── MacroBarView.swift
│           ├── HealthBadgeView.swift
│           └── StatCardView.swift
│
└── docs/
    └── superpowers/
        └── specs/
            └── 2026-04-14-fibonacci-health-design.md
```

---

## Verification Plan

### Pi App (with Test Mode)
1. Launch app — Dashboard loads, shows placeholder state (no user connected)
2. Enable Test Mode in Settings → amber banner appears on all screens
3. Select a food from picker, enter a weight → Scan Result screen shows correct calorie calculation (`calories_per_100g × weight / 100`)
4. Tap "Add to Log" → entry appears in Food Log, calorie ring updates
5. Tap "Discard" → no entry added
6. Verify health score and macro bars render correctly for a known food
7. Verify dark ↔ light theme toggle persists across screen navigation

### Pi App (with hardware)
1. Place food on scale → weight stabilises and displays on Dashboard
2. Hold food in front of HuskyLens → Scan Result appears with correct food name
3. Unmapped label ID → shows "Unknown Food" state gracefully

### iOS App
1. Grant HealthKit permissions → Activity screen shows real step/calorie data
2. Deny HealthKit permissions → app shows manual-mode fallback, no crash
3. Open Device screen, scan for Pi → Pi appears in list, tap to connect
4. Connection established → UserProfile + HealthSnapshot written to Pi; Pi updates calorie ring
5. Trigger a scan on Pi → iOS SessionState notification received → Dashboard updates remaining calories
6. Edit profile on iOS → reconnect → Pi reflects updated calorie goal

### BLE Integration
1. Connect multiple iPhones in sequence → each loads its own profile (keyed by MAC)
2. Disconnect phone → Pi retains last user session until new device connects
3. Reconnect same phone → Pi resumes correct user session
