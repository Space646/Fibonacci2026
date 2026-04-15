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
