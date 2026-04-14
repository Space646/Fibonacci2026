from collections import deque


class WeightService:
    """
    Reads weight from an HX711 loadcell.
    Pass test_mode=True to bypass hardware and use set_test_weight() instead.
    Pass skip_hardware_init=True to skip GPIO setup without test mode
    (useful for unit tests on non-Pi hardware).
    """

    STABILITY_READINGS = 5
    STABILITY_TOLERANCE_G = 2.0

    def __init__(self, test_mode: bool = False, skip_hardware_init: bool = False):
        self._test_mode = test_mode
        self._test_weight: float = 0.0
        self._is_test_stable: bool = False
        self._readings: deque = deque(maxlen=self.STABILITY_READINGS)
        self._hx = None

        if not test_mode and not skip_hardware_init:
            self._init_hardware()

    def _init_hardware(self):
        try:
            from hx711 import HX711  # type: ignore
            self._hx = HX711(dout_pin=5, pd_sck_pin=6)
            self._hx.reset()
            self._hx.tare()
        except Exception:
            pass  # Non-Pi environment; hardware reads will return 0

    def read(self) -> float:
        """
        Poll hardware once. Call at ~10 Hz from a timer.
        In test mode this is a no-op (weight is set directly via set_test_weight).
        """
        if self._test_mode or self._hx is None:
            return self._test_weight

        try:
            raw = self._hx.get_weight_mean(5)
            reading = max(0.0, round(raw, 1))
        except Exception:
            reading = 0.0

        self._readings.append(reading)
        return reading

    def set_test_weight(self, grams: float) -> None:
        """Test mode only: override the current weight reading."""
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
