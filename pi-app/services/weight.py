import json
import os
from collections import deque
from datetime import datetime, timezone


class WeightService:
    """
    Reads weight from an HX711 loadcell.
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

        self._offset: float = 0.0
        self._scale_factor: float = 1.0
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
            self._offset = float(data.get("offset", 0.0))
            self._scale_factor = float(data.get("scale_factor", 1.0))
        except (FileNotFoundError, json.JSONDecodeError, ValueError):
            self._offset = 0.0
            self._scale_factor = 1.0

    def save_calibration(self, offset: float, scale_factor: float):
        if scale_factor == 0.0:
            raise ValueError("scale_factor cannot be zero")
        self._offset = offset
        self._scale_factor = scale_factor
        os.makedirs(os.path.dirname(self._calibration_file), exist_ok=True)
        data = {
            "offset": offset,
            "scale_factor": scale_factor,
            "calibrated_at": datetime.now(timezone.utc).isoformat(),
        }
        with open(self._calibration_file, "w") as f:
            json.dump(data, f, indent=2)

    @staticmethod
    def compute_calibration(raw_zero: float, raw_point1: float, known_weight1: float,
                            raw_point2: float, known_weight2: float) -> tuple[float, float]:
        offset = raw_zero
        delta_raw = raw_point2 - raw_point1
        delta_weight = known_weight2 - known_weight1
        scale_factor = delta_raw / delta_weight if delta_weight != 0 else 1.0
        return offset, scale_factor

    def _init_hardware(self):
        try:
            from hx711 import HX711  # type: ignore
            self._hx = HX711(dout_pin=5, pd_sck_pin=6)
            self._hx.reset()
        except Exception:
            pass

    def read_raw(self) -> float:
        if self._test_mode or self._hx is None:
            return self._test_weight
        try:
            return float(self._hx.get_weight_mean(5))
        except Exception:
            return 0.0

    def read(self) -> float:
        if self._test_mode:
            return max(0.0, round((self._test_weight - self._offset) / self._scale_factor, 1))

        if self._hx is None:
            return 0.0

        try:
            raw = float(self._hx.get_weight_mean(5))
            reading = max(0.0, round((raw - self._offset) / self._scale_factor, 1))
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
