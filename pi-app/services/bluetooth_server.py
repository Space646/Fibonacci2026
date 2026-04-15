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

    Threading: the supplied callbacks fire on the BLE background thread.
    Callers are responsible for marshalling to their UI / main thread — Qt
    signal connections handle this automatically via queued delivery.

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
        self._stop_lock = threading.Lock()
        self._stopped = False
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
        # Wait up to 5 s for the server to finish its async setup.
        # `self.started` is set by `_async_setup` on the loop thread before
        # `_ready_evt.set()`, so by the time `wait` returns True it is already
        # True — avoiding a race where a notification fires between
        # `_ready_evt.set()` and the main thread setting `started = True`.
        if self._ready_evt.wait(timeout=5.0):
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

        # Writable characteristics (iOS → Pi).
        # IMPORTANT: pass value=None. CoreBluetooth raises
        # NSInternalInconsistencyException("Characteristics with cached values
        # must be read-only") if any non-read-only characteristic is created
        # with a cached value. BlueZ tolerates initial values, but for
        # portability we use None everywhere and let _on_read_request supply
        # sensible defaults.
        write_props = (
            GATTCharacteristicProperties.write
            | GATTCharacteristicProperties.write_without_response
        )
        write_perms = GATTAttributePermissions.writeable
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_USER_PROFILE, write_props, None, write_perms
        )
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_HEALTH_SNAP, write_props, None, write_perms
        )

        # Notify characteristics (Pi → iOS). `.read` is kept so iOS can pull
        # the latest value on connect before the first notification arrives;
        # the cached value restriction only triggers when an initial value is
        # supplied at creation time, so passing None is fine.
        notify_props = GATTCharacteristicProperties.notify | GATTCharacteristicProperties.read
        notify_perms = GATTAttributePermissions.readable
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_FOOD_LOG_SYNC, notify_props, None, notify_perms
        )
        await server.add_new_characteristic(
            SERVICE_UUID, CHAR_SESSION_STATE, notify_props, None, notify_perms
        )

        await server.start()
        self._server = server
        # Set `started` on the loop thread BEFORE signalling ready, so that
        # any notification scheduled by the main thread immediately after
        # `_ready_evt.wait()` returns observes `started == True`.
        self.started = True
        self._ready_evt.set()

    async def _async_teardown(self) -> None:
        if self._server is not None:
            try:
                await self._server.stop()
            except Exception:
                log.exception("BlessServer.stop() failed")

    # ── bless callbacks (called on the loop thread) ──────────────────────────

    def _on_read_request(self, characteristic, **kwargs):
        """Return the characteristic's current value on read requests.

        Characteristics are created with `value=None` (CoreBluetooth rejects
        cached values on non-read-only characteristics). If a central reads
        before the first notify has set a value, fall back to an empty
        payload shaped for the UUID.
        """
        value = getattr(characteristic, "value", None)
        if value:
            return value
        char_uuid = str(getattr(characteristic, "uuid", "")).lower()
        if char_uuid == CHAR_FOOD_LOG_SYNC.lower():
            return b"[]"
        if char_uuid == CHAR_SESSION_STATE.lower():
            return b"{}"
        return b""

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
        with self._stop_lock:
            if self._stopped:
                return
            if self._loop is None:
                self._stopped = True
                return
            try:
                self._loop.call_soon_threadsafe(self._loop.stop)
            except RuntimeError:
                pass  # loop already stopped
            if self._thread is not None:
                self._thread.join(timeout=2.0)
            self.started = False
            self._stopped = True
