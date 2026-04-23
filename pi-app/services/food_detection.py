import threading
import time
from typing import Callable, Optional


class FoodDetectionService:
    """
    Reads food label IDs from HuskyLens via I2C and resolves them to DB rows.
    In test mode, I2C is bypassed; call inject_food_by_id() to simulate a detection.
    """

    POLL_INTERVAL_S = 0.3

    def __init__(self, conn, test_mode: bool = False, i2c_address: int = 0x32,
                 on_detection: Optional[Callable] = None):
        self._conn = conn
        self._test_mode = test_mode
        self._on_detection = on_detection
        self._huskylens = None
        self._thread: Optional[threading.Thread] = None
        self._running = False
        self._last_detected_id: Optional[int] = None

        if not test_mode:
            self._start_i2c(i2c_address)

    def _start_i2c(self, address: int):
        try:
            from huskylib import HuskyLensLibrary  # type: ignore
            self._huskylens = HuskyLensLibrary("I2C", "", address=address)
            if not self._huskylens.knock():
                self._huskylens = None
                return
            self._running = True
            self._thread = threading.Thread(target=self._poll_loop, daemon=True)
            self._thread.start()
        except Exception:
            self._huskylens = None

    def _poll_loop(self):
        while self._running and self._huskylens:
            try:
                blocks = self._huskylens.blocks()
                if blocks and isinstance(blocks, list) and len(blocks) > 0:
                    label_id = blocks[0].ID
                    if label_id > 0 and label_id != self._last_detected_id:
                        self._last_detected_id = label_id
                        food = self._lookup_label(label_id)
                        if self._on_detection:
                            self._on_detection(food)
                else:
                    self._last_detected_id = None
            except Exception:
                pass
            time.sleep(self.POLL_INTERVAL_S)

    def _lookup_label(self, label_id: int) -> Optional[dict]:
        row = self._conn.execute(
            "SELECT * FROM foods WHERE huskylens_label_id = ?", (label_id,)
        ).fetchone()
        return dict(row) if row else None

    def inject_food_by_id(self, food_id: int) -> Optional[dict]:
        """Test mode: simulate detection of a food by its DB primary key."""
        row = self._conn.execute(
            "SELECT * FROM foods WHERE id = ?", (food_id,)
        ).fetchone()
        food = dict(row) if row else None
        if self._on_detection:
            self._on_detection(food)
        return food

    def get_all_foods(self) -> list[dict]:
        """Return all foods in DB for use in test mode picker."""
        rows = self._conn.execute(
            "SELECT * FROM foods ORDER BY name"
        ).fetchall()
        return [dict(r) for r in rows]

    def stop(self):
        self._running = False
