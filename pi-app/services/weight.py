import json
import os
from collections import deque
from datetime import datetime, timezone


class WeightService:
    """
    Reads weight from an HX711 load cell via tatobari/hx711py.
    Pass test_mode=True to bypass hardware and use set_test_weight() instead.
    Pass skip_hardware_init=True to skip GPIO setup without test mode.
    """

    STABILITY_READINGS = 5
    STABILITY_TOLERANCE_G = 2.0

    def __init__(self, test_mode: bool = False, skip_hardware_init: bool = False,
                 calibration_file: str | None = None):
        self._test_mode = test_mode
        self._test_weight: float = 0.0
        self._is_test_stable: bool = False
        self._readings: deque = deque(maxlen=self.STABILITY_READINGS)
        self._hx = None

        self._reference_unit: float = 1.0
        self._calibration_file = calibration_file or os.path.join(
            os.path.dirname(os.path.dirname(__file__)), "config", "scale_calibration.json"
        )
        self._load_calibration()

        if not test_mode and not skip_hardware_init:
            self._init_hardware()

    def _load_calibration(self):
        try:
            with open(self._calibration_file, "r") as f:
                data = json.load(f)
            self._reference_unit = float(data.get("reference_unit", 1.0))
        except (FileNotFoundError, json.JSONDecodeError, ValueError):
            self._reference_unit = 1.0

    def save_calibration(self, reference_unit: float):
        if reference_unit == 0.0:
            raise ValueError("reference_unit cannot be zero")
        self._reference_unit = reference_unit
        if self._hx is not None:
            self._hx.set_reference_unit(reference_unit)
        os.makedirs(os.path.dirname(self._calibration_file), exist_ok=True)
        data = {
            "reference_unit": reference_unit,
            "calibrated_at": datetime.now(timezone.utc).isoformat(),
        }
        with open(self._calibration_file, "w") as f:
            json.dump(data, f, indent=2)

    @staticmethod
    def compute_calibration(raw_reading: float, known_weight: float) -> float:
        if known_weight == 0:
            raise ValueError("known_weight cannot be zero")
        if raw_reading == 0:
            raise ValueError("raw_reading is zero — scale may not be responding")
        return raw_reading / known_weight

    def _init_hardware(self):
        try:
            from hx711 import HX711  # type: ignore
            self._hx = HX711(5, 6)
            self._hx.set_reading_format("MSB", "MSB")
            self._hx.set_reference_unit(self._reference_unit)
            self._hx.reset()
            self._hx.tare()
        except Exception:
            pass

    def tare(self):
        if self._hx is not None:
            self._hx.tare()

    def read_raw(self) -> float:
        """Returns tare-adjusted reading with reference_unit=1, for use during calibration."""
        if self._test_mode or self._hx is None:
            return self._test_weight
        try:
            self._hx.set_reference_unit(1)
            raw = float(self._hx.get_weight(5))
            return raw
        except Exception:
            return 0.0
        finally:
            self._hx.set_reference_unit(self._reference_unit)

    def read(self) -> float:
        if self._test_mode:
            return self._test_weight

        if self._hx is None:
            return 0.0

        try:
            reading = max(0.0, round(float(self._hx.get_weight(5)), 1))
        except Exception:
            reading = 0.0

        self._readings.append(reading)
        return reading

    def set_test_weight(self, grams: float) -> None:
        self._test_weight = grams
        self._is_test_stable = True

    @property
    def current_weight_g(self) -> float:
        if self._test_mode:
            return self._test_weight
        if not self._readings:
            return 0.0
        return self._readings[-1]

    @property
    def is_stable(self) -> bool:
        if self._test_mode:
            return self._is_test_stable
        if len(self._readings) < self.STABILITY_READINGS:
            return False
        spread = max(self._readings) - min(self._readings)
        return spread <= self.STABILITY_TOLERANCE_G
