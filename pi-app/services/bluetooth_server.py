import json
import threading
from typing import Callable, Optional

# UUIDs for the FiboHealth GATT service
SERVICE_UUID        = "12345678-1234-5678-1234-56789abcdef0"
CHAR_USER_PROFILE   = "12345678-1234-5678-1234-56789abcdef1"
CHAR_HEALTH_SNAP    = "12345678-1234-5678-1234-56789abcdef2"
CHAR_FOOD_LOG_SYNC  = "12345678-1234-5678-1234-56789abcdef3"
CHAR_SESSION_STATE  = "12345678-1234-5678-1234-56789abcdef4"


class BluetoothServer:
    """
    BLE GATT peripheral. Runs only on systems with BlueZ/dbus.
    on_profile_received(mac, profile_dict) — called when iOS writes UserProfile
    on_health_received(mac, snapshot_dict) — called when iOS writes HealthSnapshot
    """

    def __init__(
        self,
        on_profile_received: Optional[Callable] = None,
        on_health_received: Optional[Callable] = None,
        enabled: bool = True,
    ):
        self._on_profile = on_profile_received
        self._on_health = on_health_received
        self._clients: dict[str, object] = {}  # mac → connection
        self._enabled = enabled
        self._app = None

        if enabled:
            self._start()

    def _start(self):
        try:
            import dbus  # type: ignore
            import dbus.mainloop.glib  # type: ignore
            from gi.repository import GLib  # type: ignore
            dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
            self._loop = GLib.MainLoop()
            t = threading.Thread(target=self._run_loop, daemon=True)
            t.start()
        except ImportError:
            pass  # Not on RPi — BLE silently disabled

    def _run_loop(self):
        # Full BlueZ GATT application registration omitted for brevity;
        # implement using the bluez GATT API (register_application, register_advertisement).
        # The _handle_write method below is the key entry point.
        if self._loop:
            self._loop.run()

    def _handle_write(self, char_uuid: str, value: bytes, client_mac: str):
        """Called by BlueZ when the iOS app writes a characteristic."""
        try:
            data = json.loads(value.decode("utf-8"))
        except Exception:
            return

        if char_uuid == CHAR_USER_PROFILE and self._on_profile:
            self._on_profile(client_mac, data)
        elif char_uuid == CHAR_HEALTH_SNAP and self._on_health:
            self._on_health(client_mac, data)

    def notify_food_log(self, client_mac: str, log_entries: list) -> None:
        """Send food log entries to connected iOS client."""
        payload = json.dumps(log_entries).encode("utf-8")
        self._notify_client(client_mac, CHAR_FOOD_LOG_SYNC, payload)

    def notify_session_state(self, client_mac: str, state: dict) -> None:
        """Send session state (remaining calories etc.) to connected iOS client."""
        payload = json.dumps(state).encode("utf-8")
        self._notify_client(client_mac, CHAR_SESSION_STATE, payload)

    def _notify_client(self, mac: str, char_uuid: str, payload: bytes):
        # In full implementation, look up the characteristic object by UUID
        # and call .PropertiesChanged() to trigger notification
        pass

    def stop(self):
        if self._loop:
            self._loop.quit()
