# AntiDonut Promotion White Paper — Design Spec
*Fibonacci Robotics Olympiad 2026 · April 22, 2026*

---

## Overview

A comprehensive white paper for robotics competition judges, designed to be both technically rigorous and accessible for teammate presentations. The document tells the story of AntiDonut (Fibonacci Health) — a complete hardware-software system for intelligent nutrition tracking — while highlighting the technical innovation, product completeness, and integration complexity that judges need to understand.

**Audience:** Robotics competition judges + teammates presenting the project  
**Format:** 4–5 page white paper with 2 diagrams  
**Tone:** Professional, clear problem-to-solution narrative with technical depth on demand

---

## Document Structure

### 1. Abstract (½ page)
**Purpose:** Hook judges and summarize the entire project in one crisp paragraph.

**Content:**
- Opening: "AntiDonut is a fully integrated hardware-software nutrition tracking system..."
- Key claim: Solves the real-world problem of unreliable calorie tracking
- Key innovation: Intelligent hardware (AI food detection) + offline processing + multi-device sync
- Scope: Raspberry Pi hardware, iOS app, Android app, all working offline without internet
- Closing: "This white paper details the technical architecture, integration challenges, and validation results."

**Why this matters:** Judges immediately know what they're reading about and why it's impressive.

---

### 2. Introduction: The Problem (½ page)
**Purpose:** Establish why AntiDonut was worth building.

**Content:**
- **Current State of Nutrition Tracking:**
  - Manual calorie logging is tedious and unreliable (users guess)
  - Existing apps require internet and rely on incomplete food databases
  - No integration between hardware (scales) and software (phones)
  - Users often abandon tracking because friction is too high

- **Real-World Friction:**
  - Fresh/prepared foods aren't well-represented in generic databases
  - Portion control requires guessing without a scale
  - Fitness goals (activity syncing) are siloed from nutrition data
  - Privacy concerns: cloud-based solutions store personal health data

- **Opportunity:**
  - "What if nutrition tracking could be instant, accurate, and fully offline?"
  - Connect hardware intelligence with companion apps users already carry
  - Enable seamless health data flow between device and phone

---

### 3. Solution Overview (1 page)
**Purpose:** Explain what AntiDonut is and why the approach is clever.

**Content:**
- **The Product:**
  - A Raspberry Pi-based food scale with integrated HuskyLens K210 camera
  - Identifies food via computer vision, measures weight via load cell
  - Pairs over Bluetooth with iOS and Android companion apps
  - Syncs nutrition data, activity stats, and user profiles in real-time
  - Works completely offline — no internet required

- **Core Innovation — Three Pillars:**
  1. **Intelligent Hardware:** AI-powered food detection (not manual selection)
  2. **Offline-First Architecture:** All processing local to Pi, no cloud dependency
  3. **Multi-Device Synchronization:** Seamless BLE sync between Pi, iOS, and Android

- **Why This Approach:**
  - Hardware + AI eliminates guessing: camera sees the food
  - Offline ensures privacy, reliability, and instant feedback
  - BLE sync bridges the gap: nutrition data lives on the phone, intelligence lives on the hardware

- **Include:** System Architecture Diagram
  - Show boxes: Raspberry Pi (with HuskyLens, HX711, LCD), iOS App, Android App
  - Show connections: UART (HuskyLens), GPIO (weight), BLE (phones)
  - Caption: "AntiDonut integrates four sub-systems: hardware detection, weight measurement, local intelligence, and multi-device sync."

---

### 4. System Architecture & Data Flow (1.5 pages)
**Purpose:** Deep dive into how the pieces fit together.

**Content:**

#### **System Architecture (with diagram)**
- **Hardware Layer:**
  - Raspberry Pi 4/5: Central compute unit
  - HuskyLens K210: Computer vision for food identification (sends label IDs over UART)
  - HX711 Load Cell: Precision weight measurement (±2g stability)
  - 5" LCD Touchscreen: User interface (DSI/HDMI)

- **Communication Protocols:**
  - UART (115.2k baud): HuskyLens → Pi
  - GPIO (SPI-like): HX711 → Pi
  - BLE GATT: Pi (peripheral) ↔ iOS/Android (central)
  - SQLite3: Persistent local storage

- **Software Architecture (on Pi):**
  - **FoodDetectionService:** Listens for HuskyLens labels, queries food database
  - **WeightService:** Polls HX711 at 10 Hz, enforces stability tolerance
  - **CalorieCalculator:** Computes intake based on food × weight
  - **BluetoothServer:** GATT peripheral advertising `FiboHealth` service
  - **UserSessionManager:** Maintains multi-user state, persists logs
  - **QML UI:** PyQt6 interface on touchscreen (Dashboard, Scan Results, Food Log, Settings)

#### **Data Flow (with diagram)**
Show the journey of a food scan:
1. User places food on scale
2. HuskyLens camera detects object → sends label ID (UART)
3. FoodDetectionService queries `foods` table by ID
4. WeightService polls HX711, waits for ±2g stability
5. CalorieCalculator: `calories = food.calories_per_100g × weight_g / 100`
6. UI shows: food name, weight, total calories, macros, health badge
7. User confirms → logged to SQLite
8. BluetoothServer notifies connected iOS/Android apps
9. Apps display updated food log + remaining daily calories

**Why This Matters:**
- Judges see the *integration problem solved*: hardware, firmware, database, wireless sync all working together
- Highlights the complexity: UART + GPIO + BLE + multi-user database = non-trivial embedded systems challenge
- Shows end-to-end flow: raw sensor → processed data → user notification

---

### 5. Technical Deep Dives (1.5 pages)
**Purpose:** Showcase the engineering decisions and why they matter.

#### **A. Food Detection & AI Integration**
- **How it works:** HuskyLens K210 runs pre-trained model for food classification
- **Output:** Numeric label IDs (sent to Pi over UART)
- **Database mapping:** Pi maintains `foods` table: `huskylens_label_id → (name, calories_per_100g, macros)`
- **Accuracy:** [Team to fill: "Successfully identifies X common foods with Y% confidence"]
- **Speed:** [Team to fill: "Detection + database lookup < Z milliseconds"]
- **Why this beats manual selection:** User doesn't have to scroll menus; camera sees the food

#### **B. Hardware Integration Challenge**
- **The Problem:** Synchronizing two different hardware interfaces (UART @ 115.2k baud, GPIO @ 10 Hz polling)
- **The Solution:**
  - UART: interrupt-driven (bless handles incoming labels)
  - GPIO: polled service (WeightService loop, 10 Hz rate)
  - Both feed independent Qt signals → UI handles asynchronous updates
- **Stability Logic:** Accept weight only when 5 consecutive readings are within ±2g
  - Filters noise from movement, vibration
  - Ensures user can see "stable weight" message
- **Why this matters:** Raw hardware signals are noisy; judges need to see you handled real-world physics

#### **C. Calorie Calculation Engine**
- **Lookup:** Food database contains `calories_per_100g`
- **Formula:** `consumed = food.calories_per_100g × weight_measured_g / 100`
- **Personalized daily goal:**
  - If user set override: use that
  - Else: Mifflin-St Jeor BMR × activity multiplier
  - **BMR (male):** `10×weight_kg + 6.25×height_cm − 5×age + 5`
  - **BMR (female):** `10×weight_kg + 6.25×height_cm − 5×age − 161`
  - **Activity multipliers:** sedentary ×1.2, light ×1.375, moderate ×1.55, active ×1.725
- **Real-time display:** UI shows consumed, remaining, and activity contribution
- **Why this matters:** Shows you handle real nutritional science, not just "scan + log"

#### **D. BLE Multi-Device Synchronization**
- **Architecture:** Pi advertises as GATT peripheral (`FiboHealth` service)
- **iOS/Android:** Act as BLE central (client)
- **Sync protocol:**
  - On connect: phone sends user profile (name, age, weight, height, activity level) + HealthKit data (daily steps, active minutes, calories burned)
  - Pi responds with: food log for today, session state, remaining calories
  - On new scan: Pi notifies connected clients in real-time
- **Multi-user support:** Identified by Bluetooth MAC address
  - Each user gets isolated session, daily log, and calorie goal
  - Guest mode: transient user (MAC = `00:00:00:00:00:00`) for unpaired scans
- **Why this matters:** BLE is notoriously tricky; judges see you handled async notifications, connection lifecycle, and data consistency across three platforms

#### **E. Offline-First Architecture**
- **Database:** SQLite3 on Pi (not cloud)
- **All processing:** Happens locally (food detection, calorie calc, user session management)
- **Why offline:**
  - **Privacy:** No food logs sent to servers
  - **Reliability:** Works even if WiFi/cell is down
  - **Latency:** Instant feedback (no network round-trips)
  - **Competitive advantage:** Users own their data
- **Data persistence:** Food logs synced to iOS HealthKit / Android health platforms (user's choice)
- **Why this matters:** Judges appreciate architecture thinking — you didn't just build an app, you made a deliberate choice about data and privacy

---

### 6. Implementation Highlights (½ page)
**Purpose:** Show that all three platforms were built, not just one.

**Content:**
- **Raspberry Pi App (Python 3.11):**
  - PyQt6 + QML for all UI screens
  - BlueZ + `dbus-python` for BLE peripheral
  - `pyserial` for UART (HuskyLens)
  - `hx711` library for load cell
  - SQLite3 for persistence
  - Services: FoodDetectionService, WeightService, CalorieCalculator, BluetoothServer, UserSessionManager

- **iOS App (Swift/SwiftUI):**
  - Native BLE client (CoreBluetooth)
  - HealthKit integration (read activity, write nutrition data)
  - Real-time food log display
  - User profile sync (age, weight, height, activity)
  - Offline-capable (cached data if BLE drops)

- **Android App (Kotlin):**
  - Jetpack Compose for UI
  - Android BLE APIs for GATT client
  - Google Fit / Health Connect integration
  - Feature parity with iOS
  - Background service for BLE scanning

- **Integration Challenge:** Maintaining consistent behavior across three platforms with different constraints
  - iOS: HealthKit ecosystem, App Store review process
  - Android: Fragmented BLE stack, permission model
  - Pi: Real-time hardware constraints, Bluetooth Classic + BLE interop

---

### 7. Results & Validation (½ page)
**Purpose:** Show the system actually works.

**Content:**
- **Testing Scope:**
  - Hardware accuracy: HX711 stability, HuskyLens detection rate
  - BLE reliability: Connection lifecycle, multi-device sync, notification delivery
  - Data consistency: User profiles sync correctly, food logs persist across sessions
  - Edge cases: Low battery, network drop, concurrent users, out-of-bounds inputs

- **Validation Metrics (team to fill with actual numbers):**
  - Food detection: "___ common foods identified with ___% confidence"
  - Weight precision: "HX711 stable readings within ±2g after ___ ms"
  - BLE sync latency: "Food log updates arrive on phone in < ___ seconds"
  - Multi-user isolation: "Verified ___ concurrent users, each with isolated sessions"
  - Offline operation: "___ days of functionality without internet"

- **Test Modes & Instrumentation:**
  - Test Mode toggle: Replaces UART input with manual picker, GPIO with text input
  - Visible test banner: Prevents accidental use of fake data in production
  - Debug logging: Timestamps and state transitions for troubleshooting

---

### 8. Conclusion (¼ page)
**Purpose:** Remind judges why this is impressive.

**Content:**
- **What was delivered:**
  - A *complete, end-to-end system* — not a demo or prototype
  - Hardware integration: sensors, custom calibration, real-time processing
  - Software breadth: Pi app, iOS, Android, cross-platform sync
  - Engineering depth: BLE middleware, multi-user database, offline architecture

- **Why judges should be impressed:**
  - Solving a real problem (nutrition tracking is broken)
  - Technical scope: 3 platforms, 3 communication protocols, hardware + software
  - Thoughtful design: offline-first, privacy, multi-user
  - Execution: working system, tested, ready for extension

- **Future directions (optional):**
  - More food recognition models
  - Meal planning integration
  - Apple Watch / Wear OS support
  - Market deployment

---

## Diagrams

### Diagram 1: System Architecture
**Visual elements:**
- Box: Raspberry Pi (center)
  - Inside: HuskyLens K210, HX711, LCD Touchscreen, Pi Compute
- Box: iOS App (left, connected by BLE)
- Box: Android App (right, connected by BLE)
- Lines labeled: UART (HuskyLens), GPIO (HX711), DSI (LCD), BLE GATT (phones)
- Database icon: SQLite3 (inside Pi)

**Caption:** "AntiDonut architecture: hardware sensors feed into the Pi, which broadcasts data to iOS and Android over BLE. All processing is local; no internet required."

---

### Diagram 2: Data Flow (Food Scan to User Notification)
**Flow:**
```
HuskyLens Camera
       ↓
   Food Detection (UART)
       ↓
   Database Lookup (SQLite)
       ↓
HX711 Weight Measurement (GPIO)
       ↓
Calorie Calculation (math)
       ↓
QML UI Display (Pi screen)
       ↓
BLE Notification (to iPhone/Android)
       ↓
User Food Log (app sync)
```

**Caption:** "When a user places food on the scale, AntiDonut detects it via camera, measures it via load cell, calculates calories locally, and syncs the result to the user's phone—all within seconds, no internet required."

---

## Tone & Voice Guidelines

- **Accessible layer:** Problem statements ("manual tracking is tedious"), high-level explanations ("camera sees the food")
- **Technical layer:** Component details, architecture choices, validation metrics
- **Judges can skim** section intros and conclusions; **deep-dive** into technical sections if interested
- **Active voice:** "We designed X to solve Y" (not "X was designed")
- **Confidence:** Avoid hedging ("hopefully," "maybe"); judges want to see conviction

---

## Deliverables

1. **White paper document** (4–5 pages, markdown or PDF)
   - All sections filled in with team's actual metrics
   - Diagrams embedded or referenced
   - Ready for teammate presentation

2. **Diagrams** (2 files)
   - System Architecture Diagram (SVG or high-res PNG)
   - Data Flow Diagram (SVG or high-res PNG)

3. **Metrics to gather from team:**
   - Food detection accuracy (% confidence)
   - Weight measurement stability (±X grams)
   - BLE sync latency (milliseconds)
   - Concurrent user test results
   - Other validation data

---

## Success Criteria

✅ White paper is 4–5 pages (not too long, not too short)  
✅ Clear problem statement (judges understand why this matters)  
✅ Accessible overview + technical depth (both audiences served)  
✅ Diagrams illustrate architecture and data flow (not just decorative)  
✅ Validation section has real metrics (proves it works)  
✅ Teammates can present this with confidence (understandable + knowledgeable)  
✅ Judges see scope, integration complexity, and execution quality  

---
