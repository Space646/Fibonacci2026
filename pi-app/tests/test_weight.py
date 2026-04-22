import json
import pytest
from services.weight import WeightService


def test_test_mode_returns_set_weight():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(150.0)
    assert svc.current_weight_g == 150.0

def test_test_mode_default_zero():
    svc = WeightService(test_mode=True)
    assert svc.current_weight_g == 0.0

def test_set_test_weight_updates_value():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(200.0)
    svc.set_test_weight(99.5)
    assert svc.current_weight_g == 99.5

def test_stability_requires_five_readings():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(100.0)
    assert svc.is_stable is True

def test_hardware_mode_not_stable_initially():
    svc = WeightService(test_mode=False, skip_hardware_init=True)
    assert svc.is_stable is False
    assert svc.current_weight_g == 0.0


def test_load_calibration_from_json(tmp_path):
    cal_file = tmp_path / "scale_calibration.json"
    cal_file.write_text(json.dumps({
        "reference_unit": 2.5,
        "calibrated_at": "2026-04-21T12:00:00"
    }))
    svc = WeightService(test_mode=False, skip_hardware_init=True,
                        calibration_file=str(cal_file))
    assert svc._reference_unit == 2.5


def test_load_calibration_missing_file():
    svc = WeightService(test_mode=False, skip_hardware_init=True,
                        calibration_file="/nonexistent/path.json")
    assert svc._reference_unit == 1.0


def test_read_raw_returns_test_weight():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(200.0)
    assert svc.read_raw() == 200.0


def test_read_returns_test_weight_directly():
    svc = WeightService(test_mode=True)
    svc.set_test_weight(150.0)
    assert svc.read() == 150.0


def test_save_calibration(tmp_path):
    cal_file = tmp_path / "scale_calibration.json"
    svc = WeightService(test_mode=True, calibration_file=str(cal_file))
    svc.save_calibration(reference_unit=2.5)
    data = json.loads(cal_file.read_text())
    assert data["reference_unit"] == 2.5
    assert "calibrated_at" in data


def test_save_calibration_rejects_zero(tmp_path):
    cal_file = tmp_path / "scale_calibration.json"
    svc = WeightService(test_mode=True, calibration_file=str(cal_file))
    with pytest.raises(ValueError):
        svc.save_calibration(reference_unit=0.0)


def test_compute_calibration_from_single_point():
    ref_unit = WeightService.compute_calibration(raw_reading=500.0, known_weight=200.0)
    assert ref_unit == 2.5


def test_compute_calibration_rejects_zero_weight():
    with pytest.raises(ValueError):
        WeightService.compute_calibration(raw_reading=500.0, known_weight=0.0)


def test_compute_calibration_rejects_zero_raw():
    with pytest.raises(ValueError):
        WeightService.compute_calibration(raw_reading=0.0, known_weight=200.0)
