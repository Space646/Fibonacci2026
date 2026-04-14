# BLE Connection Completion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the Pi ↔ iPhone BLE link so an iPhone can discover the Pi, sync the user's profile + HealthKit snapshot, and receive live food-log / session-state updates. Works on macOS (dev) and Raspberry Pi (deploy), with platform detection handled by the `bless` library.

**Architecture:** On the Pi, rewrite `BluetoothServer` as a sync facade around an asyncio `bless` peripheral running in a background thread; the public API (`on_profile_received`, `on_health_received`, `notify_food_log`, `notify_session_state`, `stop`) stays identical so `app_state.py` needs no structural changes. On iOS, add a persistent `deviceId` UUID to `UserProfile`, include it in every BLE write, and expose `resync()` so edits made while connected can be pushed without reconnecting.

**Tech Stack:** Python 3.11+, `bless>=0.2.6` (async cross-platform GATT peripheral), PyQt6 signals, SQLite (via existing `UserSessionManager`), Swift 5.9+, CoreBluetooth, SwiftData. Spec: `docs/superpowers/specs/2026-04-14-ble-connection-completion-design.md`.

---

## File Structure

**Pi (Python):**
- Modify: `pi-app/requirements.txt` — add `bless>=0.2.6`
- Rewrite: `pi-app/services/bluetooth_server.py` — bless-based peripheral, sync facade over async core, all four characteristics
- Modify: `pi-app/services/user_session.py` — rename parameter `bluetooth_mac` → `device_id`, rename `get_user_by_mac` → `get_user_by_device`
- Modify: `pi-app/ui/app_state.py` — call-site rename, pass device_id from BLE writes through to session manager
- Create: `pi-app/tests/test_bluetooth_server.py` — tests for the pure-Python routing / degradation paths (BLE layer itself is not unit tested)
- Create: `pi-app/README.md` — setup notes for macOS dev and Raspberry Pi deployment

**iOS (Swift):**
- Modify: `ios-app/FiboHealth/Models/UserProfile.swift` — add `deviceId: UUID`, include in `blePayload()`
- Modify: `ios-app/FiboHealth/Services/BluetoothClient.swift` — internal `sendProfile`/`sendSnapshot`, add `pendingDeviceId`, modify `sendSnapshot` to inject device_id, add `resync()`
- Modify: `ios-app/FiboHealth/AppEnvironment.swift` — set `pendingDeviceId` in `init` and `syncToPi`, call `bluetooth.resync()` from `syncToPi()`
- Modify: `ios-app/FiboHealthTests/FiboHealthTests.swift` — add `testUserProfileBLEPayloadIncludesDeviceId`

---

## Task 1: Add `bless` to Pi requirements

**Files:**
- Modify: `pi-app/requirements.txt`

- [ ] **Step 1: Edit `pi-app/requirements.txt`**

Add `bless>=0.2.6` as a new line. Final contents:

```
PyQt6>=6.6.0
pyserial>=3.5
pytest>=8.0
pytest-qt>=4.4
bless>=0.2.6
```

- [ ] **Step 2: Install the new dependency**

Run:
```bash
cd pi-app && pip install -r requirements.txt
```
Expected: `bless` and its transitive deps (`dbus-fast` on Linux, `bleak` / CoreBluetooth wrappers on macOS) install without error.

- [ ] **Step 3: Verify bless imports**

Run:
```bash
python -c "from bless import BlessServer, GATTCharacteristicProperties, GATTAttributePermissions; print('OK')"
```
Expected: `OK`.

- [ ] **Step 4: Commit**

```bash
git add pi-app/requirements.txt
git commit -m "feat(pi): add bless dependency for cross-platform GATT peripheral"
```

---

## Task 2: Rename `bluetooth_mac` → `device_id` in `UserSessionManager`

**Files:**
- Modify: `pi-app/services/user_session.py`
- Test: `pi-app/tests/test_user_session.py` (existing — positional calls keep working, but update one keyword-arg usage if present)

The database column stays `bluetooth_mac` (zero schema migration); only the Python parameter name at the API boundary changes.

- [ ] **Step 1: Run existing tests to confirm baseline**

Run:
```bash
cd pi-app && pytest tests/test_user_session.py -v
```
Expected: All 6 tests pass.

- [ ] **Step 2: Rename parameters in `services/user_session.py`**

Replace the file's current contents with:

```python
import sqlite3
from typing import Optional


GUEST_MAC = "00:00:00:00:00:00"  # sentinel device_id for the guest session


class UserSessionManager:
    def __init__(self, conn: sqlite3.Connection):
        self._conn = conn

    def upsert_user(self, device_id: str, profile: dict) -> dict:
        self._conn.execute(
            """INSERT INTO users
               (bluetooth_mac, name, age, weight_kg, height_cm, sex,
                activity_level, daily_calorie_goal)
               VALUES (:mac,:name,:age,:weight_kg,:height_cm,:sex,
                       :activity_level,:daily_calorie_goal)
               ON CONFLICT(bluetooth_mac) DO UPDATE SET
                 name=excluded.name, age=excluded.age,
                 weight_kg=excluded.weight_kg, height_cm=excluded.height_cm,
                 sex=excluded.sex, activity_level=excluded.activity_level,
                 daily_calorie_goal=excluded.daily_calorie_goal""",
            {"mac": device_id, **profile},
        )
        self._conn.commit()
        row = self._conn.execute(
            "SELECT * FROM users WHERE bluetooth_mac = ?", (device_id,)
        ).fetchone()
        return dict(row)

    def get_user_by_device(self, device_id: str) -> Optional[dict]:
        row = self._conn.execute(
            "SELECT * FROM users WHERE bluetooth_mac = ?", (device_id,)
        ).fetchone()
        return dict(row) if row else None

    def log_food(self, user_id: int, food_id: int,
                 weight_g: float, calories: float) -> dict:
        cursor = self._conn.execute(
            """INSERT INTO food_log (user_id, food_id, weight_g, calories)
               VALUES (?,?,?,?)""",
            (user_id, food_id, weight_g, calories),
        )
        self._conn.commit()
        row = self._conn.execute(
            "SELECT * FROM food_log WHERE id = ?", (cursor.lastrowid,)
        ).fetchone()
        return dict(row)

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
        return [dict(r) for r in rows]

    def total_calories_today(self, user_id: int) -> float:
        result = self._conn.execute(
            """SELECT COALESCE(SUM(calories), 0)
               FROM food_log
               WHERE user_id = ? AND date(timestamp) = date('now')""",
            (user_id,),
        ).fetchone()[0]
        return float(result)

    def delete_log_entry(self, entry_id: int) -> None:
        self._conn.execute("DELETE FROM food_log WHERE id = ?", (entry_id,))
        self._conn.commit()

    def update_health_snapshot(self, user_id: int, snapshot: dict) -> None:
        """Store today's HealthKit data received from iOS app."""
        self._conn.execute(
            """INSERT OR REPLACE INTO health_snapshots
               (user_id, date, steps, calories_burned, active_minutes, workouts)
               VALUES (:user_id, :date, :steps, :calories_burned,
                       :active_minutes, :workouts)""",
            {"user_id": user_id, **snapshot},
        )
        self._conn.commit()

    def get_health_snapshot(self, user_id: int) -> Optional[dict]:
        row = self._conn.execute(
            """SELECT * FROM health_snapshots
               WHERE user_id = ? AND date = date('now')""",
            (user_id,),
        ).fetchone()
        return dict(row) if row else None
```

The only changes are:
- `upsert_user(bluetooth_mac, ...)` → `upsert_user(device_id, ...)`
- `get_user_by_mac(bluetooth_mac)` → `get_user_by_device(device_id)`

Everything else, including SQL column names and the `GUEST_MAC` constant, is unchanged.

- [ ] **Step 3: Run tests again — they still pass because callers use positional args**

Run:
```bash
cd pi-app && pytest tests/test_user_session.py -v
```
Expected: All 6 tests still pass (they use positional args, so the rename is source-compatible at the test level).

- [ ] **Step 4: Commit**

```bash
git add pi-app/services/user_session.py
git commit -m "refactor(pi): rename bluetooth_mac -> device_id in session manager"
```

---

## Task 3: Add tests for `BluetoothServer` routing + degradation

**Files:**
- Create: `pi-app/tests/test_bluetooth_server.py`

These tests cover the pure-Python surface of `BluetoothServer` — payload routing, no-op paths, startup degradation. They do **not** exercise any real BLE I/O. Tests run on macOS and Linux without any Bluetooth hardware.

- [ ] **Step 1: Write the failing tests**

Create `pi-app/tests/test_bluetooth_server.py` with:

```python
import json
import pytest
from services.bluetooth_server import (
    BluetoothServer,
    SERVICE_UUID,
    CHAR_USER_PROFILE,
    CHAR_HEALTH_SNAP,
    CHAR_FOOD_LOG_SYNC,
    CHAR_SESSION_STATE,
)


def test_disabled_server_is_noop():
    """enabled=False must not start a thread or import bless."""
    server = BluetoothServer(enabled=False)
    assert server.started is False
    # Must not raise
    server.notify_food_log("device-1", [])
    server.notify_session_state("device-1", {})
    server.stop()


def test_route_write_user_profile_invokes_callback():
    received = []
    server = BluetoothServer(
        on_profile_received=lambda did, data: received.append(("profile", did, data)),
        enabled=False,
    )
    payload = json.dumps({
        "device_id": "ABC-123",
        "name": "Alice",
        "age": 30,
        "weight_kg": 60,
        "height_cm": 165,
        "sex": "female",
        "activity_level": "moderate",
        "daily_calorie_goal": None,
    }).encode("utf-8")

    server._route_write(CHAR_USER_PROFILE, payload)

    assert len(received) == 1
    kind, did, data = received[0]
    assert kind == "profile"
    assert did == "ABC-123"
    assert data["name"] == "Alice"
    assert "device_id" not in data  # stripped before handoff


def test_route_write_health_snapshot_invokes_callback():
    received = []
    server = BluetoothServer(
        on_health_received=lambda did, data: received.append(("health", did, data)),
        enabled=False,
    )
    payload = json.dumps({
        "device_id": "ABC-123",
        "date": "2026-04-14",
        "steps": 5000,
        "calories_burned": 250,
        "active_minutes": 30,
        "workouts": 1,
    }).encode("utf-8")

    server._route_write(CHAR_HEALTH_SNAP, payload)

    assert len(received) == 1
    kind, did, data = received[0]
    assert kind == "health"
    assert did == "ABC-123"
    assert data["steps"] == 5000
    assert "device_id" not in data


def test_route_write_drops_missing_device_id():
    received = []
    server = BluetoothServer(
        on_profile_received=lambda did, data: received.append(did),
        enabled=False,
    )
    payload = json.dumps({"name": "Alice"}).encode("utf-8")  # no device_id

    server._route_write(CHAR_USER_PROFILE, payload)

    assert received == []  # callback never fired


def test_route_write_drops_invalid_json():
    received = []
    server = BluetoothServer(
        on_profile_received=lambda did, data: received.append(did),
        enabled=False,
    )

    server._route_write(CHAR_USER_PROFILE, b"\x00\x01not-json")

    assert received == []


def test_route_write_ignores_notify_only_characteristics():
    """Writes to FoodLogSync or SessionState should be dropped silently."""
    received = []
    server = BluetoothServer(
        on_profile_received=lambda did, data: received.append(did),
        on_health_received=lambda did, data: received.append(did),
        enabled=False,
    )
    payload = json.dumps({"device_id": "X"}).encode("utf-8")

    server._route_write(CHAR_FOOD_LOG_SYNC, payload)
    server._route_write(CHAR_SESSION_STATE, payload)

    assert received == []


def test_notify_methods_safe_when_server_not_started():
    """notify_* must not raise even if the bless server never started."""
    server = BluetoothServer(enabled=False)
    server.notify_food_log("device-1", [{"id": 1, "food_name": "Apple"}])
    server.notify_session_state("device-1", {"calories_consumed": 100})
    # No assertion needed — the test passes if no exception is raised
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:
```bash
cd pi-app && pytest tests/test_bluetooth_server.py -v
```
Expected: All tests FAIL with `AttributeError` (`_route_write` doesn't exist yet, `started` attribute doesn't exist, etc.) — this is normal; we implement in Task 4.

- [ ] **Step 3: Commit the failing tests**

```bash
git add pi-app/tests/test_bluetooth_server.py
git commit -m "test(pi): add BluetoothServer routing and degradation tests (red)"
```

---

## Task 4: Rewrite `bluetooth_server.py` with bless

**Files:**
- Rewrite: `pi-app/services/bluetooth_server.py`

This is the core implementation. The class keeps the exact same public API it has today (`on_profile_received` / `on_health_received` callbacks, `notify_food_log` / `notify_session_state` / `stop` methods) plus one added property (`started`) that the tests check.

- [ ] **Step 1: Replace `pi-app/services/bluetooth_server.py` with the full implementation**

```python
"""BLE GATT peripheral for FiboHealth.

Sync facade over an asyncio bless server. The bless library handles the
per-platform backend (CoreBluetooth on macOS, BlueZ/D-Bus on Linux/RPi).

Public API matches the sync, signal-slot style the rest of the Pi app uses:
callbacks fire from a background thread; Qt signals emitted from those
callbacks are safely marshalled to the main thread by Qt's queued-connection
mechanism.
"""

from __future__ import annotations

import asyncio
import json
import logging
import platform
import threading
from typing import Callable, Optional

log = logging.getLogger(__name__)

# UUIDs for the FiboHealth GATT service — must match iOS BluetoothClient
SERVICE_UUID        = "12345678-1234-5678-1234-56789abcdef0"
CHAR_USER_PROFILE   = "12345678-1234-5678-1234-56789abcdef1"
CHAR_HEALTH_SNAP    = "12345678-1234-5678-1234-56789abcdef2"
CHAR_FOOD_LOG_SYNC  = "12345678-1234-5678-1234-56789abcdef3"
CHAR_SESSION_STATE  = "12345678-1234-5678-1234-56789abcdef4"

PERIPHERAL_NAME = "FiboHealth-Pi"


class BluetoothServer:
    """BLE GATT peripheral.

    Parameters
    ----------
    on_profile_received : callable(device_id: str, profile: dict)
        Invoked when iOS writes the UserProfile characteristic.
    on_health_received : callable(device_id: str, snapshot: dict)
        Invoked when iOS writes the HealthSnapshot characteristic.
    enabled : bool
        If False, the server is a complete no-op (no thread, no bless import).
        Use for tests and for Test Mode on the Pi app.
    """

    def __init__(
        self,
        on_profile_received: Optional[Callable[[str, dict], None]] = None,
        on_health_received: Optional[Callable[[str, dict], None]] = None,
        enabled: bool = True,
    ):
        self._on_profile = on_profile_received
        self._on_health = on_health_received
        self._enabled = enabled
        self._loop: Optional[asyncio.AbstractEventLoop] = None
        self._thread: Optional[threading.Thread] = None
        self._server = None  # bless BlessServer, populated on the loop thread
        self._ready_evt = threading.Event()
        self.started = False

        if enabled:
            self._start()

    # ── Lifecycle ────────────────────────────────────────────────────────────

    def _start(self) -> None:
        """Launch the asyncio loop in a background thread and bring bless up."""
        log.info("BLE peripheral: starting on %s", platform.system())
        try:
            import bless  # noqa: F401 — import check only
        except ImportError as exc:
            log.warning("BLE unavailable — import failed: %s", exc)
            return

        self._thread = threading.Thread(
            target=self._thread_main, name="BluetoothServer", daemon=True
        )
        self._thread.start()
        # Wait up to 5 s for the server to finish its async setup
        if self._ready_evt.wait(timeout=5.0):
            self.started = True
            log.info("BLE peripheral: advertising as %r", PERIPHERAL_NAME)
        else:
            log.warning("BLE peripheral: startup timed out (5s)")

    def _thread_main(self) -> None:
        """Run the asyncio loop for the lifetime of the server."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        self._loop = loop
        try:
            loop.run_until_complete(self._async_setup())
            loop.run_forever()
        except Exception:
            log.exception("BLE peripheral crashed")
        finally:
            try:
                loop.run_until_complete(self._async_teardown())
            except Exception:
                log.exception("BLE teardown error")
            loop.close()

    async def _async_setup(self) -> None:
        """Build the BlessServer, register service + characteristics, start advertising."""
        from bless import (
            BlessServer,
            GATTCharacteristicProperties,
            GATTAttributePermissions,
        )

        server = BlessServer(name=PERIPHERAL_NAME, loop=self._loop)
        server.read_request_func = self._on_read_request
        server.write_request_func = self._on_write_request

        # Service
        await server.add_new_service(SERVICE_UUID)

        # Writable characteristics (iOS → Pi)
        write_props = (
            GATTCharacteristicProperties.write
            | GATTCharacteristicProperties.write_without_response
        )
        write_perms = GATTAttributePermissions.writeable
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_USER_PROFILE, write_props, b"", write_perms
        )
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_HEALTH_SNAP, write_props, b"", write_perms
        )

        # Notify characteristics (Pi → iOS)
        notify_props = GATTCharacteristicProperties.notify | GATTCharacteristicProperties.read
        notify_perms = GATTAttributePermissions.readable
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_FOOD_LOG_SYNC, notify_props, b"[]", notify_perms
        )
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_SESSION_STATE, notify_props, b"{}", notify_perms
        )

        await server.start()
        self._server = server
        self._ready_evt.set()

    async def _async_teardown(self) -> None:
        if self._server is not None:
            try:
                await self._server.stop()
            except Exception:
                log.exception("BlessServer.stop() failed")

    # ── bless callbacks (called on the loop thread) ──────────────────────────

    def _on_read_request(self, characteristic, **kwargs):
        """Return the characteristic's current cached value on read requests."""
        return characteristic.value

    def _on_write_request(self, characteristic, value, **kwargs):
        """Dispatched by bless on every write. Route by UUID."""
        try:
            char_uuid = str(characteristic.uuid).lower()
        except Exception:
            return
        self._route_write(char_uuid, bytes(value))

    # ── Routing (pure Python, unit-tested) ───────────────────────────────────

    def _route_write(self, char_uuid: str, value: bytes) -> None:
        """Decode a write payload and dispatch to the appropriate callback.

        Pure Python — no bless types in its signature, so it is unit-testable.
        """
        char_uuid = char_uuid.lower()
        try:
            data = json.loads(value.decode("utf-8"))
        except Exception:
            log.warning("BLE write: invalid JSON, dropped")
            return

        if not isinstance(data, dict):
            log.warning("BLE write: non-object JSON, dropped")
            return

        device_id = data.pop("device_id", None)
        if not device_id:
            log.warning("BLE write: missing device_id, dropped")
            return

        if char_uuid == CHAR_USER_PROFILE.lower():
            if self._on_profile is not None:
                self._on_profile(device_id, data)
        elif char_uuid == CHAR_HEALTH_SNAP.lower():
            if self._on_health is not None:
                self._on_health(device_id, data)
        # Writes to notify-only characteristics are silently dropped.

    # ── Notifications (called from Qt main thread) ───────────────────────────

    def notify_food_log(self, device_id: str, log_entries: list) -> None:
        """Send food log array to connected iOS central.

        `device_id` is accepted for API symmetry but not used — bless delivers
        notifications to all currently-subscribed centrals (we only ever expect
        one active phone in practice).
        """
        payload = json.dumps(log_entries, default=str).encode("utf-8")
        self._schedule_notify(CHAR_FOOD_LOG_SYNC, payload)

    def notify_session_state(self, device_id: str, state: dict) -> None:
        """Send session-state object to connected iOS central."""
        payload = json.dumps(state, default=str).encode("utf-8")
        self._schedule_notify(CHAR_SESSION_STATE, payload)

    def _schedule_notify(self, char_uuid: str, payload: bytes) -> None:
        if not self.started or self._loop is None or self._server is None:
            return  # No-op when BLE is disabled / not up
        try:
            asyncio.run_coroutine_threadsafe(
                self._do_notify(char_uuid, payload), self._loop
            )
        except Exception:
            log.exception("Failed to schedule BLE notification")

    async def _do_notify(self, char_uuid: str, payload: bytes) -> None:
        try:
            char = self._server.get_characteristic(char_uuid)
            if char is None:
                log.warning("BLE notify: characteristic %s not found", char_uuid)
                return
            char.value = payload
            self._server.update_value(SERVICE_UUID, char_uuid)
        except Exception:
            log.exception("BLE notify for %s failed", char_uuid)

    # ── Shutdown ─────────────────────────────────────────────────────────────

    def stop(self) -> None:
        """Stop advertising and tear down the server. Safe to call repeatedly."""
        if self._loop is None:
            return
        try:
            self._loop.call_soon_threadsafe(self._loop.stop)
        except RuntimeError:
            pass  # loop already stopped
        if self._thread is not None:
            self._thread.join(timeout=2.0)
        self.started = False
```

- [ ] **Step 2: Run the tests from Task 3 — they must now pass**

Run:
```bash
cd pi-app && pytest tests/test_bluetooth_server.py -v
```
Expected: All 7 tests PASS.

- [ ] **Step 3: Run the full test suite — nothing else regresses**

Run:
```bash
cd pi-app && pytest -v
```
Expected: Every test passes; no import errors.

- [ ] **Step 4: Smoke-test that the server actually starts on macOS**

Run:
```bash
cd pi-app && python -c "
import logging, time
logging.basicConfig(level=logging.INFO)
from services.bluetooth_server import BluetoothServer
s = BluetoothServer(enabled=True)
print('started =', s.started)
time.sleep(1.0)
s.stop()
print('stopped OK')
"
```
Expected output (on a Mac with Bluetooth permissions granted; on first run macOS will prompt for Bluetooth access — accept):
```
INFO:services.bluetooth_server:BLE peripheral: starting on Darwin
INFO:services.bluetooth_server:BLE peripheral: advertising as 'FiboHealth-Pi'
started = True
stopped OK
```
If the script hangs or prints `started = False`, check that the OS has granted Bluetooth permission to the Python binary (System Settings → Privacy & Security → Bluetooth).

- [ ] **Step 5: Commit**

```bash
git add pi-app/services/bluetooth_server.py
git commit -m "feat(pi): cross-platform BLE GATT peripheral via bless"
```

---

## Task 5: Thread device_id through `app_state.py`

**Files:**
- Modify: `pi-app/ui/app_state.py`

The BLE callbacks already accept `(mac, profile)` / `(mac, snapshot)` signatures — we just rename the variable and update the one call to `upsert_user`. The old `get_user_by_mac` isn't called from app_state (verified), so no change needed there beyond the rename in Task 2.

- [ ] **Step 1: Open `pi-app/ui/app_state.py` and update the BLE callbacks**

Find:
```python
    # ── BLE Callbacks ────────────────────────────────────────────────────────

    def _on_profile_received(self, mac: str, profile: dict):
        self._active_user = self._session.upsert_user(mac, profile)
        self.userChanged.emit()
        self._sync_to_client(mac)

    def _on_health_received(self, mac: str, snapshot: dict):
        self._session.update_health_snapshot(self._active_user["id"], snapshot)
        self.userChanged.emit()
        self._sync_to_client(mac)
```

Replace with:
```python
    # ── BLE Callbacks ────────────────────────────────────────────────────────

    def _on_profile_received(self, device_id: str, profile: dict):
        self._active_user = self._session.upsert_user(device_id, profile)
        self.userChanged.emit()
        self._sync_to_client(device_id)

    def _on_health_received(self, device_id: str, snapshot: dict):
        self._session.update_health_snapshot(self._active_user["id"], snapshot)
        self.userChanged.emit()
        self._sync_to_client(device_id)
```

Find:
```python
    def _sync_to_client(self, mac: str):
        log = self._session.get_todays_log(self._active_user["id"])
        self._ble.notify_food_log(mac, log)
        self._ble.notify_session_state(mac, {
```

Replace with:
```python
    def _sync_to_client(self, device_id: str):
        log = self._session.get_todays_log(self._active_user["id"])
        self._ble.notify_food_log(device_id, log)
        self._ble.notify_session_state(device_id, {
```

The rest of `_sync_to_client` is unchanged.

- [ ] **Step 2: Run the full pytest suite to check nothing broke**

Run:
```bash
cd pi-app && pytest -v
```
Expected: All tests pass.

- [ ] **Step 3: Smoke-test the app boots**

Run:
```bash
cd pi-app && python main.py --test
```
Expected: Qt window opens, Dashboard loads, amber "TEST MODE" banner visible, no tracebacks in the terminal. Close the window (`Cmd-Q`).

- [ ] **Step 4: Commit**

```bash
git add pi-app/ui/app_state.py
git commit -m "refactor(pi): rename mac -> device_id in BLE callback plumbing"
```

---

## Task 6: Add `deviceId` to iOS `UserProfile` model

**Files:**
- Modify: `ios-app/FiboHealth/Models/UserProfile.swift`
- Modify: `ios-app/FiboHealthTests/FiboHealthTests.swift`

- [ ] **Step 1: Write the failing test**

Replace `ios-app/FiboHealthTests/FiboHealthTests.swift` with:

```swift
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
```

- [ ] **Step 2: Regenerate the Xcode project and run tests to verify failure**

Run:
```bash
cd ios-app && xcodegen generate && \
  xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: `testUserProfileBLEPayloadIncludesDeviceId` FAILS with "blePayload must include device_id"; `testUserProfileBLEPayloadContainsAllFields` passes.

(If `xcodegen` is not installed: `brew install xcodegen`. If no iPhone 15 simulator: substitute any installed iOS 17 simulator — `xcrun simctl list devices` to see options.)

- [ ] **Step 3: Update `Models/UserProfile.swift` to include deviceId**

Replace the file's contents with:

```swift
import SwiftData
import Foundation

@Model
final class UserProfile {
    var id: UUID
    var deviceId: UUID
    var name: String
    var age: Int
    var weightKg: Double
    var heightCm: Double
    var sex: String          // "male" | "female" | "other"
    var activityLevel: String // "sedentary" | "light" | "moderate" | "active"
    var dailyCalorieGoal: Double?  // nil = use calculated BMR
    var createdAt: Date

    init(name: String = "", age: Int = 25, weightKg: Double = 70,
         heightCm: Double = 170, sex: String = "other",
         activityLevel: String = "moderate", dailyCalorieGoal: Double? = nil) {
        self.id = UUID()
        self.deviceId = UUID()
        self.name = name
        self.age = age
        self.weightKg = weightKg
        self.heightCm = heightCm
        self.sex = sex
        self.activityLevel = activityLevel
        self.dailyCalorieGoal = dailyCalorieGoal
        self.createdAt = Date()
    }

    /// JSON payload sent to Pi over BLE
    func blePayload() -> [String: Any] {
        [
            "device_id": deviceId.uuidString,
            "name": name,
            "age": age,
            "weight_kg": weightKg,
            "height_cm": heightCm,
            "sex": sex,
            "activity_level": activityLevel,
            "daily_calorie_goal": dailyCalorieGoal as Any
        ]
    }

    /// Calculated daily calorie goal using Mifflin-St Jeor BMR
    var calculatedDailyGoal: Double {
        if let manual = dailyCalorieGoal { return manual }
        let base = 10 * weightKg + 6.25 * heightCm - 5 * Double(age)
        let bmr: Double
        switch sex {
        case "male":   bmr = base + 5
        case "female": bmr = base - 161
        default:       bmr = base - 78
        }
        let multipliers = ["sedentary": 1.2, "light": 1.375, "moderate": 1.55, "active": 1.725]
        return (bmr * (multipliers[activityLevel] ?? 1.2)).rounded()
    }
}
```

- [ ] **Step 4: Run the tests — they must now pass**

Run:
```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: Both tests PASS.

- [ ] **Step 5: Commit**

```bash
git add ios-app/FiboHealth/Models/UserProfile.swift \
        ios-app/FiboHealthTests/FiboHealthTests.swift
git commit -m "feat(ios): persistent deviceId on UserProfile for BLE identification"
```

---

## Task 7: Update `BluetoothClient.swift` — `pendingDeviceId`, modified `sendSnapshot`, `resync`

**Files:**
- Modify: `ios-app/FiboHealth/Services/BluetoothClient.swift`

- [ ] **Step 1: Replace the file contents**

Replace `ios-app/FiboHealth/Services/BluetoothClient.swift` with:

```swift
import CoreBluetooth
import Combine
import Foundation

// Must match Pi's bluetooth_server.py UUIDs exactly
private let serviceUUID       = CBUUID(string: "12345678-1234-5678-1234-56789abcdef0")
private let charUserProfile   = CBUUID(string: "12345678-1234-5678-1234-56789abcdef1")
private let charHealthSnap    = CBUUID(string: "12345678-1234-5678-1234-56789abcdef2")
private let charFoodLogSync   = CBUUID(string: "12345678-1234-5678-1234-56789abcdef3")
private let charSessionState  = CBUUID(string: "12345678-1234-5678-1234-56789abcdef4")

final class BluetoothClient: NSObject, ObservableObject {
    @Published var isConnected: Bool = false
    @Published var isScanning: Bool = false
    @Published var foodLog: [FoodLogEntry] = []
    @Published var sessionState: SessionState = .empty
    @Published var lastSyncTime: Date?

    private var central: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var characteristics: [CBUUID: CBCharacteristic] = [:]

    // Data to send on next connect (set before scan); also used by resync()
    var pendingProfile: [String: Any]?
    var pendingSnapshot: HealthSnapshot?
    var pendingDeviceId: String?

    override init() {
        super.init()
        central = CBCentralManager(delegate: self, queue: nil)
    }

    func startScanning() {
        guard central.state == .poweredOn else { return }
        isScanning = true
        central.scanForPeripherals(withServices: [serviceUUID], options: nil)
    }

    func disconnect() {
        if let p = peripheral { central.cancelPeripheralConnection(p) }
    }

    /// Re-push the current profile + snapshot to the Pi without disconnecting.
    /// Safe to call when not connected (no-op).
    func resync() {
        guard isConnected else { return }
        sendProfile()
        sendSnapshot()
    }

    // MARK: - Writes (internal so AppEnvironment/resync can trigger them)

    func sendProfile() {
        guard let char = characteristics[charUserProfile],
              var payload = pendingProfile else { return }
        // Ensure device_id is present — pendingProfile from UserProfile.blePayload
        // already includes it, but inject pendingDeviceId as a fallback.
        if payload["device_id"] == nil, let did = pendingDeviceId {
            payload["device_id"] = did
        }
        guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return }
        peripheral?.writeValue(data, for: char, type: .withResponse)
    }

    func sendSnapshot() {
        guard let char = characteristics[charHealthSnap],
              let snap = pendingSnapshot,
              let deviceId = pendingDeviceId else { return }
        let payload: [String: Any] = [
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
}

// MARK: - CBCentralManagerDelegate

extension BluetoothClient: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn { startScanning() }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData: [String: Any], rssi RSSI: NSNumber) {
        self.peripheral = peripheral
        central.stopScan()
        isScanning = false
        central.connect(peripheral, options: nil)
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        isConnected = true
        peripheral.delegate = self
        peripheral.discoverServices([serviceUUID])
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral,
                        error: Error?) {
        isConnected = false
        characteristics = [:]
        // Auto-reconnect
        startScanning()
    }
}

// MARK: - CBPeripheralDelegate

extension BluetoothClient: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let service = peripheral.services?.first(where: { $0.uuid == serviceUUID }) else { return }
        peripheral.discoverCharacteristics(
            [charUserProfile, charHealthSnap, charFoodLogSync, charSessionState],
            for: service
        )
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        service.characteristics?.forEach { char in
            characteristics[char.uuid] = char
            if char.uuid == charFoodLogSync || char.uuid == charSessionState {
                peripheral.setNotifyValue(true, for: char)
            }
        }
        // Sync on connect
        sendProfile()
        sendSnapshot()
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard let data = characteristic.value else { return }

        if characteristic.uuid == charFoodLogSync {
            if let entries = try? JSONDecoder().decode([FoodLogEntry].self, from: data) {
                DispatchQueue.main.async { self.foodLog = entries }
            }
        } else if characteristic.uuid == charSessionState {
            if let state = try? JSONDecoder().decode(SessionState.self, from: data) {
                DispatchQueue.main.async {
                    self.sessionState = state
                    self.lastSyncTime = Date()
                }
            }
        }
    }
}
```

Key changes from the previous version:
- `sendProfile` and `sendSnapshot` are `func` (internal) instead of `private`
- Added `pendingDeviceId: String?` property
- `sendSnapshot` builds the JSON payload by hand and injects `device_id`
- Added `resync()` method
- `sendProfile` falls back to `pendingDeviceId` if the passed-in `pendingProfile` dict somehow lacks `device_id`

- [ ] **Step 2: Build the iOS app to verify compilation**

Run:
```bash
cd ios-app && xcodegen generate && \
  xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: `BUILD SUCCEEDED`.

- [ ] **Step 3: Re-run the existing tests to confirm no regression**

Run:
```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: Both `testUserProfileBLEPayloadIncludesDeviceId` and `testUserProfileBLEPayloadContainsAllFields` pass.

- [ ] **Step 4: Commit**

```bash
git add ios-app/FiboHealth/Services/BluetoothClient.swift
git commit -m "feat(ios): expose sendProfile/sendSnapshot + add resync() and pendingDeviceId"
```

---

## Task 8: Wire `pendingDeviceId` and `resync()` in `AppEnvironment.swift`

**Files:**
- Modify: `ios-app/FiboHealth/AppEnvironment.swift`

- [ ] **Step 1: Replace `AppEnvironment.swift` with the wired-up version**

```swift
import SwiftUI
import SwiftData

@MainActor
final class AppEnvironment: ObservableObject {
    @Published var theme: Theme = .dark

    let profileStore: UserProfileStore
    let healthKit: HealthKitService
    let bluetooth: BluetoothClient

    init(modelContext: ModelContext) {
        self.profileStore = UserProfileStore(modelContext: modelContext)
        self.healthKit = HealthKitService()
        self.bluetooth = BluetoothClient()

        // Wire up: BLE pending payloads start from the current profile + snapshot
        self.bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        self.bluetooth.pendingProfile = profileStore.profile.blePayload()
        self.bluetooth.pendingSnapshot = healthKit.snapshot

        // Request HealthKit on startup
        self.healthKit.requestAuthorization()
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
}
```

- [ ] **Step 2: Build**

Run:
```bash
cd ios-app && xcodebuild build -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: `BUILD SUCCEEDED`.

- [ ] **Step 3: Commit**

```bash
git add ios-app/FiboHealth/AppEnvironment.swift
git commit -m "feat(ios): wire pendingDeviceId and resync() in AppEnvironment"
```

---

## Task 9: Write Pi README with setup notes

**Files:**
- Create: `pi-app/README.md`

- [ ] **Step 1: Create `pi-app/README.md`**

```markdown
# FiboHealth Pi App

Nutrition-tracking food scale built on Raspberry Pi with a HuskyLens camera and
HX711 load cell. Pairs over BLE with the iOS companion app.

## Running

### Test mode (any platform, no hardware)

```bash
pip install -r requirements.txt
python main.py --test
```

Enables the in-app Test Mode toggle automatically, bypasses HuskyLens UART and
HX711 GPIO, and disables the BLE peripheral (use the Settings → "Simulate phone
pairing" button to exercise the connected-user UI).

### Normal mode (macOS for development)

```bash
pip install -r requirements.txt
python main.py
```

On first launch macOS asks permission to use Bluetooth — accept. The app will
advertise as `FiboHealth-Pi` and an iPhone running FiboHealth can find it from
its Device tab.

### Normal mode (Raspberry Pi OS)

```bash
sudo apt install bluez
pip install -r requirements.txt
python main.py
```

The default `pi` user is already in the `bluetooth` group. No systemd unit is
required; the app talks to BlueZ over the user session D-Bus bus.

## Troubleshooting

- **Python logs `BLE unavailable — import failed`**: `bless` didn't install.
  Re-run `pip install -r requirements.txt` and check for a Python version
  mismatch (bless needs 3.10+).
- **macOS: `started = False` after startup**: Bluetooth permission was denied
  for the Python binary. Grant it in System Settings → Privacy & Security →
  Bluetooth.
- **Pi: BlueZ errors in `journalctl --user`**: Make sure `bluetoothd` is
  running (`sudo systemctl status bluetooth`) and your Pi's BT adapter is up
  (`bluetoothctl power on`).
- **iPhone never discovers the Pi**: Confirm the Pi is advertising with
  `bluetoothctl scan le` from another Linux box, or check `started = True` in
  the Pi app logs.
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/README.md
git commit -m "docs(pi): add setup and troubleshooting README"
```

---

## Task 10: Manual integration verification

This task has no code changes — it's the test plan you run on real devices.

- [ ] **Step 1: Mac ↔ iPhone dev loop**

On Mac:
```bash
cd pi-app && python main.py
```
Grant Bluetooth permission if prompted. Confirm logs show:
```
BLE peripheral: starting on Darwin
BLE peripheral: advertising as 'FiboHealth-Pi'
```

On a physical iPhone running FiboHealth (must be a real device — simulator has no Bluetooth):
1. Open the app → Device tab → tap "Scan for Pi".
2. **Expected:** within 2–3 s the status flips to "Pi Connected" and the green dot lights up.
3. The Pi Qt window's Dashboard now shows the iPhone user's name and calorie goal (from `UserProfile.blePayload()`).
4. On the Pi, open Settings → enable Test Mode → inject any food with a weight → Add to Log. Confirm the iPhone's Dashboard calorie ring updates immediately (via `SessionState` notification).
5. On the iPhone, Profile tab → change weight → tap Save. Confirm the Pi's calorie ring updates immediately (no disconnect).
6. Close the iOS app → Pi shows "Connect your phone" prompt within ~5 s (disconnect detected).
7. Reopen iOS app → auto-reconnects, state is preserved.

- [ ] **Step 2: Degradation paths**

- Run `python main.py --test` → BLE peripheral does NOT start (`enabled=not test_mode`). Settings → Simulate phone pairing still works.
- Uninstall bless (`pip uninstall bless -y`) → `python main.py` starts normally, logs `BLE unavailable — import failed`, Dashboard stays in guest mode, no crash. Reinstall: `pip install bless>=0.2.6`.

- [ ] **Step 3: Regression sweep**

```bash
cd pi-app && pytest -v
```
Expected: every test passes.

```bash
cd ios-app && xcodebuild test -project FiboHealth.xcodeproj -scheme FiboHealth \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```
Expected: all tests pass.

- [ ] **Step 4: Raspberry Pi deployment (if Pi hardware is available)**

Repeat Step 1 on the Pi. Additionally verify:
- `journalctl --user -xe` shows no BlueZ errors during advertising.
- Rebooting the Pi and re-running `python main.py` restarts advertising correctly.
- The `FoodDetectionService` and `WeightService` continue to operate with BLE active (the Pi's async BLE thread doesn't starve the Qt main thread).

---

*End of plan. Implementing the 10 tasks in order gives a working Pi ↔ iPhone BLE link on macOS and Raspberry Pi OS, with the existing Qt UI, SwiftUI views, SQLite schema, and simulated-pairing dev path all unchanged.*
