# Secret Admin Menu

## Summary

Replace the visible "Test Mode" toggle in the Settings tab with a hidden admin menu activated by long-pressing the Dashboard refresh button for 5 seconds. The menu provides four administrative actions: close app, manually add food, update & restart, and calibrate the weight scale.

## Trigger

- Long-press (5 seconds) on the refresh button in `Dashboard.qml`
- Uses a QML `Timer` that starts on press, cancels on release before 5s
- Normal short-tap behavior (`refreshHome()`) is preserved
- If the timer fires, the admin overlay opens

## Admin Overlay UI

- Full-screen overlay: semi-transparent black backdrop (`#000000` at 85% opacity)
- Centered card with title "Admin Menu"
- Four stacked action buttons:
  1. Close App
  2. Add Food Item
  3. Update & Restart
  4. Calibrate Scale
- "X" button in top-right corner of card to dismiss without action
- Styled with existing dark theme (indigo/cyan gradients for action buttons)
- Overlay is a sibling to `StackView` in `main.qml` with high z-index

## Menu Actions

### Close App

- Calls `appState.stopService()`
- Backend runs `systemctl stop fibonacci-health.service` via subprocess

### Add Food Item

- Replaces menu content with inline food picker
- ComboBox populated from `appState.allFoods`
- Numeric TextField for weight in grams
- "Confirm" button calls `appState.setTestWeight(grams)` then `appState.injectFood(foodId)`
- Works regardless of test mode state
- "Back" button returns to main admin menu

### Update & Restart

- Calls `appState.updateAndRestart()`
- Backend runs `git pull` in project directory, then `systemctl restart fibonacci-health.service`

### Calibrate Scale

- Replaces menu content with a 3-step wizard
- **Step 1 (Tare):** "Remove everything from the scale" + "Next" button. Records raw zero reading.
- **Step 2 (First reference):** "Place a known weight on the scale" + TextField for weight in grams + "Confirm". Records raw reading.
- **Step 3 (Second reference):** "Place a second item (keep the first)" + TextField for combined weight in grams + "Confirm". Records raw reading.
- After step 3: computes linear scale factor from three points (0g, weight1, weight2), saves to `pi-app/config/scale_calibration.json`
- Shows "Calibration complete" confirmation, then returns to admin menu
- "Cancel" button available at each step to abort

## Calibration Persistence

- File: `pi-app/config/scale_calibration.json`
- Schema:
  ```json
  {
    "offset": <raw_zero_reading>,
    "scale_factor": <computed_factor>,
    "calibrated_at": "<ISO timestamp>"
  }
  ```
- `WeightService` loads this file on init; applies offset and scale factor in `read()`
- `WeightService` exposes `read_raw()` for uncalibrated readings during calibration

## Backend Slots (AppState)

New `@pyqtSlot` methods:

- `stopService()` — `systemctl stop fibonacci-health.service`
- `updateAndRestart()` — `git pull` then `systemctl restart fibonacci-health.service`
- `calibrateTare()` — reads raw value, stores as zero offset
- `calibratePoint(known_grams: float)` — reads raw value paired with known weight
- `finalizeCalibration()` — computes scale factor, writes JSON, reloads WeightService

## Removals

- Settings.qml: remove entire "Test Mode" section (switch + food picker + weight field + scan button)
- main.qml: remove test banner rectangle and its anchor reference
- StackView top anchor reverts to `parent.top`
- `testMode` / `setTestMode` remain in AppState for CLI `--test` flag but are no longer exposed in UI

## Files Modified

- `pi-app/ui/main.qml` — remove test banner, add admin overlay component
- `pi-app/ui/screens/Dashboard.qml` — add long-press timer to refresh button
- `pi-app/ui/screens/Settings.qml` — remove test mode section
- `pi-app/ui/app_state.py` — add admin slots (stopService, updateAndRestart, calibrate*)
- `pi-app/services/weight.py` — add calibration loading, `read_raw()`, apply offset/factor
- `pi-app/config/scale_calibration.json` — new file (created by calibration)

## Files Created

- `pi-app/ui/components/AdminOverlay.qml` — the overlay component
