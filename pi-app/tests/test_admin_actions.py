import subprocess
from unittest.mock import patch, MagicMock
import pytest

from ui.app_state import AppState


@pytest.fixture
def app_state():
    with patch("ui.app_state.BluetoothServer"):
        state = AppState(test_mode=True)
    yield state
    state._conn.close()


def test_stop_service_calls_systemctl(app_state):
    with patch("subprocess.run") as mock_run:
        app_state.stopService()
        mock_run.assert_called_once_with(
            ["systemctl", "stop", "fibonacci-health.service"],
            check=False
        )


def test_update_and_restart_calls_git_pull_then_restart(app_state):
    with patch("subprocess.run") as mock_run:
        app_state.updateAndRestart()
        calls = mock_run.call_args_list
        assert len(calls) == 2
        assert "pull" in calls[0][0][0]
        assert "restart" in calls[1][0][0]


def test_calibrate_tare_stores_raw_reading(app_state):
    app_state._weight_svc.set_test_weight(123.0)
    app_state.calibrateTare()
    assert app_state._cal_raw_zero == 123.0


def test_calibrate_point_stores_reading(app_state):
    app_state._cal_raw_zero = 0.0
    app_state._weight_svc.set_test_weight(456.0)
    app_state.calibratePoint(200.0)
    assert app_state._cal_points[-1] == (456.0, 200.0)


def test_finalize_calibration(app_state, tmp_path):
    cal_file = tmp_path / "cal.json"
    app_state._weight_svc._calibration_file = str(cal_file)
    app_state._cal_raw_zero = 100.0
    app_state._cal_points = [(300.0, 100.0), (500.0, 200.0)]
    result = app_state.finalizeCalibration()
    assert result is True
    assert app_state._weight_svc._offset == 100.0
    assert app_state._weight_svc._scale_factor == 2.0
