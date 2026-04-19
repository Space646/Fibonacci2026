# Remove Health Score — Design

**Date:** 2026-04-19  
**Status:** Approved

## Summary

Remove the numeric `health_score` field from every platform. The binary Healthy/Unhealthy flag (`is_healthy`) is retained everywhere.

## Scope

### Pi App (Python + QML)
- `pi-app/database/schema.sql` — drop `health_score REAL DEFAULT 50` from `foods` table
- `pi-app/database/seed_foods.py` — remove `compute_health_score()` function and all call sites
- `pi-app/services/user_session.py` — remove `f.health_score` from the `get_todays_log` SELECT
- `pi-app/ui/screens/ScanResult.qml` — remove the Health Score bar `Row` block (lines ~112–135)
- `pi-app/tests/test_database.py` — remove `health_score` from column assertion; delete `test_health_score_*` tests

### Android App (Kotlin)
- `android-app/.../data/model/FoodLogEntry.kt` — remove `healthScore: Float` field and deserializer line
- `android-app/.../FoodLogEntryDecoderTest.kt` — remove `health_score` from test JSON strings

### iOS App (Swift)
- `ios-app/FiboHealth/Models/FoodLogEntry.swift` — remove `healthScore` property, `CodingKeys` case, init parameter, and decode line
- `ios-app/FiboHealthTests/FoodLogEntryDecodeTests.swift` — remove `health_score` from test JSON strings

## Out of Scope
- No database migration script (fresh installs only)
- `is_healthy` / Healthy/Unhealthy badge remains untouched
