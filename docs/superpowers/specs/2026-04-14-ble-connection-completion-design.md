# Pi ↔ iPhone BLE Connection — Completion Design
*Fibonacci Health · April 14, 2026*

---

## Overview

Finish the Bluetooth Low Energy link between the Raspberry Pi app and the FiboHealth iOS app. The Pi side currently ships as a stub (`bluetooth_server.py` registers no GATT application, never advertises, and `_notify_client` is a `pass`); the iOS side is mostly functional but cannot re-sync after the initial connection. This spec turns both into a working cross-platform peripheral/central pair.

The scope is deliberately narrow: BLE wiring only. No changes to Qt views, QML screens, SwiftUI views, the SQLite schema, or any non-BLE Pi service.

---

## Goals

1. Raspberry Pi app advertises the `FiboHealth` GATT service on both **macOS** (for development) and **Linux / Raspberry Pi OS** (for deployment), with automatic platform detection.
2. iPhone running FiboHealth discovers the Pi, connects, writes `UserProfile` + `HealthSnapshot`, and receives `FoodLogSync` + `SessionState` notifications — fully replacing the current `simulatePhonePairing` dev stub.
3. The existing public API of `BluetoothServer` (`on_profile_received`, `on_health_received`, `notify_food_log`, `notify_session_state`, `stop`) does not change — `app_state.py` keeps working untouched.
4. Users are identified by a stable `device_id` UUID generated on the iPhone, not by BT MAC (iOS uses rotating random addresses so MAC-keying is fundamentally broken).
5. Profile / health changes made while already connected can be re-pushed to the Pi without a disconnect/reconnect cycle.

## Non-Goals

- No changes to the SQLite schema (the `bluetooth_mac` column is semantically repurposed to hold the iOS-generated `device_id` UUID).
- No multi-phone concurrent sessions. One active phone at a time; notifications go to whichever central is subscribed.
- No background BLE scanning on iOS. The `bluetooth-central` background mode stays as currently configured; foreground-only operation is fine for the olympiad demo.
- No automated tests of the BLE layer itself. Integration-tested manually per the verification plan. The existing `enabled=False` no-op path stays available for unit tests elsewhere.

---

## Architecture

### Pi side — thread-wrapped async

```
┌──────────────────────── AppState (Qt main thread) ────────────────────────┐
│   BluetoothServer (sync facade)                                           │
│      ├─ __init__(enabled=True) ─► spawns daemon thread                    │
│      │                                                                    │
│      │       Background thread (owns asyncio loop)                        │
│      │       ┌─────────────────────────────────────────────┐              │
│      │       │  bless.BlessServer(name="FiboHealth-Pi")    │              │
│      │       │    ├─ Service(SERVICE_UUID)                 │              │
│      │       │    ├─ Characteristic(CHAR_USER_PROFILE,     │              │
│      │       │    │       write, write_request_handler)    │              │
│      │       │    ├─ Characteristic(CHAR_HEALTH_SNAP, …)   │              │
│      │       │    ├─ Characteristic(CHAR_FOOD_LOG_SYNC,    │              │
│      │       │    │       notify)                          │              │
│      │       │    └─ Characteristic(CHAR_SESSION_STATE,    │              │
│      │       │            notify)                          │              │
│      │       └─────────────────────────────────────────────┘              │
│      │                                                                    │
│      ├─ notify_food_log(device_id, entries) ──► run_coroutine_threadsafe  │
│      └─ notify_session_state(device_id, state) ─► run_coroutine_threadsafe│
│                                                                           │
│   Write handlers marshal back via Qt signal → _on_profile_received /      │
│   _on_health_received run on Qt main thread (safe for DB + signals).      │
└───────────────────────────────────────────────────────────────────────────┘
```

**Why thread-wrapped async rather than `qasync`:** the rest of the Pi app is sync / signal-slot Qt. `bless` is async-native. A dedicated asyncio loop in its own thread is the smallest intersection — no rewrite of `main.py`, `AppState`, or any service.

### iOS side — resync capability

```
AppEnvironment.syncToPi()
    ├─ bluetooth.pendingProfile = profileStore.profile.blePayload()
    ├─ bluetooth.pendingSnapshot = healthKit.snapshot
    └─ bluetooth.resync()            ← NEW
           ├─ guard isConnected
           ├─ sendProfile()           ← was private, now internal
           └─ sendSnapshot()          ← was private, now internal
```

No other iOS plumbing changes.

---

## Components

### `pi-app/services/bluetooth_server.py` (rewritten)

**Public API (unchanged from current skeleton):**
```python
class BluetoothServer:
    def __init__(
        self,
        on_profile_received: Optional[Callable[[str, dict], None]] = None,
        on_health_received: Optional[Callable[[str, dict], None]] = None,
        enabled: bool = True,
    ): ...

    def notify_food_log(self, device_id: str, log_entries: list) -> None: ...
    def notify_session_state(self, device_id: str, state: dict) -> None: ...
    def stop(self) -> None: ...
```

Note the parameter rename in the public API: `client_mac` → `device_id`. `app_state.py` already passes a MAC today; the call sites will pass the iOS-generated UUID string instead. The string type is unchanged so this is a semantic rename only.

**Internal behavior:**

1. `__init__` stores callbacks, detects platform, logs `"BLE peripheral: starting on {platform.system()}"`.
2. If `enabled=False` — skip startup, remain a no-op (preserves existing test / non-Pi behavior).
3. If `enabled=True` — try importing `bless`; on `ImportError` log a warning and degrade to no-op.
4. On successful import — start a daemon thread. The thread:
   - Creates a new asyncio event loop with `asyncio.new_event_loop()`, sets it as current.
   - Builds a `BlessServer` instance with name `"FiboHealth-Pi"`.
   - Registers the `FiboHealth` service and four characteristics (two writable, two notifying) via `server.add_new_service(SERVICE_UUID)` / `server.add_new_characteristic(...)`.
   - Assigns `server.read_request_func` and `server.write_request_func`.
   - `await server.start()` — this starts advertising on both macOS and Linux.
   - Runs `loop.run_forever()` so the loop stays alive for scheduled notify coroutines.
5. `_handle_write(char_uuid, value)` (called from the async loop):
   - JSON-decode `value.decode("utf-8")` — on failure, log and return.
   - Extract `device_id = data.pop("device_id", None)` from *every* payload. If missing, log and drop the write (the existing guest fallback already covers the unpaired UI state).
   - If `char_uuid == CHAR_USER_PROFILE`: call `self._on_profile(device_id, data)`.
   - If `char_uuid == CHAR_HEALTH_SNAP`: call `self._on_health(device_id, data)`.
   - Rationale: every write carries its own `device_id` so the server is stateless between writes. Writes can arrive in any order, and the server doesn't need to remember the last profile write to dispatch the next health write.
6. `notify_food_log` / `notify_session_state` — not called from the loop thread:
   - JSON-encode the payload.
   - `asyncio.run_coroutine_threadsafe(self._do_notify(char_uuid, payload), self._loop)`.
   - `_do_notify` updates the characteristic value on the server and calls `server.update_value(SERVICE_UUID, char_uuid)` to trigger BLE notification.
7. `stop()` — schedules `server.stop()` on the loop, then calls `loop.call_soon_threadsafe(loop.stop)`, then `thread.join(timeout=2.0)`.

**Thread-safety notes:**
- All `bless` / asyncio interaction happens on the background thread.
- Callbacks `on_profile_received` / `on_health_received` may be invoked from the bless loop thread. `app_state.py` currently calls directly into `UserSessionManager` (SQLite) and emits Qt signals. SQLite is thread-safe per-connection in serialized mode (Python's default), and Qt signals are thread-safe (queued when crossing threads). So we invoke the callbacks directly from the loop thread — Qt handles the marshalling.
- Notify methods are called from the Qt main thread but execute on the loop thread via `run_coroutine_threadsafe`. Safe.

### `pi-app/services/user_session.py` (one-line rename)

- Function parameters named `bluetooth_mac` become `device_id` at the API boundary. The column name in SQLite stays `bluetooth_mac` — no migration. This keeps the `GUEST_MAC` constant and the existing `SIMULATED:...` simulation path working.
- Scope of rename: `upsert_user(bluetooth_mac, ...)` → `upsert_user(device_id, ...)`, `get_user_by_mac(bluetooth_mac)` → `get_user_by_device(device_id)`. Update the one caller in `app_state.py`.

### `pi-app/requirements.txt`

Add:
```
bless>=0.2.6
```

`bless` brings in its own platform backends (`dbus-fast` on Linux, CoreBluetooth wrappers on macOS). No system packages beyond the OS-provided BlueZ on Linux.

### `ios-app/FiboHealth/Models/UserProfile.swift`

Add persistent device identifier:
```swift
@Attribute(.unique) var deviceId: UUID
```
- Assigned in `init` with `UUID()` — SwiftData persists it, so the same value survives app relaunches.
- `blePayload()` adds `"device_id": deviceId.uuidString`.

### `ios-app/FiboHealth/Services/BluetoothClient.swift`

- Change `sendProfile()` and `sendSnapshot()` from `private` to internal (no access modifier).
- `sendSnapshot()` must include `device_id` in its outgoing payload. Since `HealthSnapshot` is a clean local model and shouldn't carry a BLE-specific field, the client builds a dictionary at send time:
  ```swift
  private func sendSnapshot() {
      guard let char = characteristics[charHealthSnap],
            let snap = pendingSnapshot,
            let deviceId = pendingDeviceId else { return }
      var payload: [String: Any] = [
          "device_id": deviceId,
          "date": snap.date,
          "steps": snap.steps,
          "calories_burned": snap.caloriesBurned,
          "active_minutes": snap.activeMinutes,
          "workouts": snap.workouts,
      ]
      guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return }
      peripheral?.writeValue(data, for: char, type: .withResponse)
  }
  ```
  This replaces the current `JSONEncoder().encode(snap)` call.
- Add a `pendingDeviceId: String?` property, set by `AppEnvironment` alongside `pendingProfile` / `pendingSnapshot`.
- Add:
  ```swift
  func resync() {
      guard isConnected else { return }
      sendProfile()
      sendSnapshot()
  }
  ```

### `ios-app/FiboHealth/AppEnvironment.swift`

- Set `bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString` in `init` and in `syncToPi()`.
- Replace the `// Trigger a re-connect write...` stub in `syncToPi()` with `bluetooth.resync()`.

---

## Data Flow (happy path)

1. **iOS app launches** → `BluetoothClient` initializes `CBCentralManager`, starts scanning for `SERVICE_UUID` once BT powers on.
2. **Pi app launches** → `BluetoothServer` starts its thread, `BlessServer` begins advertising with name `"FiboHealth-Pi"` + `SERVICE_UUID` in the advertisement.
3. **iOS discovers Pi** → `centralManager(_:didDiscover:...)`, stops scan, issues `connect`.
4. **Connection established** → iOS discovers service → discovers characteristics → subscribes to `FoodLogSync` + `SessionState` → writes `UserProfile` then `HealthSnapshot` (with `device_id` in payload).
5. **Pi receives profile write** → `_handle_write` decodes JSON → `on_profile_received(device_id, profile)` fires on the Qt side → `UserSessionManager.upsert_user(device_id, profile)` → active user updated → `AppState._sync_to_client(device_id)` calls `notify_food_log` + `notify_session_state`.
6. **Notifications delivered** → iOS `peripheral(_:didUpdateValueFor:...)` decodes JSON → `@Published` properties update → SwiftUI dashboard rerenders.
7. **Profile edit on iOS** → `ProfileView` saves → `AppEnvironment.syncToPi()` → `bluetooth.resync()` → Pi receives fresh profile → cycle repeats from step 5.
8. **iOS leaves range / backgrounds fully** → iOS disconnects → Pi's connected-central list drops → no-op until next scan.
9. **iOS returns / reopens app** → existing auto-reconnect path (`didDisconnectPeripheral` → `startScanning`) re-enters at step 3.

---

## Error Handling

| Failure | Behavior |
|---|---|
| `import bless` fails at startup | Log `"BLE unavailable — import failed"`, remain no-op. App keeps running; iOS simply never finds it. |
| Platform has no BLE peripheral support (e.g. older macOS without permissions granted) | `BlessServer.start()` raises → log `"BLE peripheral could not start: {err}"`, no-op. |
| Write payload not valid JSON | Log and drop. Bless's write handler returns normally; iOS sees a successful write response (BLE doesn't surface the decode failure to the client, which is fine — iOS will retry on next connect). |
| `device_id` missing from profile payload | Log warning, drop the write. iOS should always include it after the model change; this is a defensive log only. |
| `update_value` fails | Log and drop. Next scan (which happens on every food log change) will retry. |
| `stop()` called while thread already dead | Safe — `thread.join(timeout=2.0)` returns immediately. |

Nothing bubbles up as an uncaught exception to Qt. The app degrades to "BLE not working" rather than crashing.

---

## Platform Detection

`bless` abstracts platforms internally, so we don't branch on platform for GATT code. We do log the platform at startup, and we document the setup steps for each:

- **macOS (dev)** — on first run, macOS prompts the user to grant Bluetooth permission to the Python binary. The user must accept. No other setup.
- **Linux / Raspberry Pi OS** — requires `bluez` (`sudo apt install bluez`), D-Bus running (default on Raspberry Pi OS), and the user running the app must be in the `bluetooth` group (already the case for the default `pi` user). Documented in a small `pi-app/README.md` setup section.
- **Other** — `BluetoothServer` logs `"BLE peripheral: unsupported platform {…}"` and no-ops.

---

## File / Module Changes

```
pi-app/
  requirements.txt                   (+1 line: bless>=0.2.6)
  services/
    bluetooth_server.py              (REWRITE — replaces the 80-line skeleton)
    user_session.py                  (parameter rename only)
  ui/
    app_state.py                     (update caller names + now passes device_id)
  README.md                          (NEW — Pi BLE setup + macOS dev notes)

ios-app/FiboHealth/
  Models/
    UserProfile.swift                (add deviceId: UUID, include in blePayload)
  Services/
    BluetoothClient.swift            (drop private on sendProfile/sendSnapshot,
                                      add resync())
  AppEnvironment.swift               (syncToPi → bluetooth.resync())
```

---

## Verification Plan

### Mac dev loop (no Pi)

1. `cd pi-app && pip install -r requirements.txt`
2. `python main.py` on the Mac. Grant Bluetooth permission when prompted.
3. Open FiboHealth on a physical iPhone (simulator has no Bluetooth), tap Device tab → Scan for Pi.
4. **Expected:** Pi appears within 2–3 s, status flips to "Pi Connected".
5. Pi Dashboard shows the iOS user's profile data (name, calorie goal derived from BMR).
6. iOS Dashboard shows calorie ring filling based on logged food (use Pi's Test Mode to inject a scan).
7. Edit profile on iOS (change weight) → tap Save → Pi Dashboard updates immediately (no disconnect).
8. Close iOS app → Pi returns to guest state within a few seconds.

### Raspberry Pi deployment

1. Same steps as above but running on the Pi with real hardware.
2. Additionally verify: `journalctl --user -xe` shows no BlueZ errors.
3. Test after Pi reboot — advertisement restarts automatically.

### Degradation paths

1. Run `python main.py` on a platform bless doesn't support — app starts normally, Dashboard shows "Connect your phone" indefinitely, no crash.
2. Run with `bless` uninstalled — app starts normally, log shows `"BLE unavailable — import failed"`.
3. Run `python main.py --test` — BLE server is disabled (`enabled=not test_mode`), `simulatePhonePairing` still works from the Settings screen.

### Regression check

1. All existing pytest tests still pass (`pytest pi-app`).
2. Xcode build for iOS target succeeds; no new warnings.
3. Existing simulated-pairing path (`simulatePhonePairing` in `app_state.py`) still works end-to-end.

---

## Open Decisions Resolved

- **Library:** `bless` (approved).
- **Test target:** both macOS and Raspberry Pi, auto-detected (approved).
- **User key:** `device_id` UUID generated on iOS, sent as part of `UserProfile` JSON. SQLite column kept as `bluetooth_mac` for zero-migration reuse (approved).
- **Concurrency model:** sync facade, async core on background thread (approved).
- **iOS scope:** expose `sendProfile`/`sendSnapshot`, add `resync()`, wire `AppEnvironment.syncToPi()`. No UI changes (approved).
