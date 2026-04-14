import threading
from typing import Callable, Optional


class FoodDetectionService:
    """
    Reads food label IDs from HuskyLens via UART and resolves them to DB rows.
    In test mode, UART is bypassed; call inject_food_by_id() to simulate a detection.
    """

    def __init__(self, conn, test_mode: bool = False, port: str = "/dev/ttyUSB0",
                 on_detection: Optional[Callable] = None):
        self._conn = conn
        self._test_mode = test_mode
        self._on_detection = on_detection  # callback(food_row | None)
        self._serial = None
        self._thread: Optional[threading.Thread] = None
        self._running = False

        if not test_mode:
            self._start_uart(port)

    def _start_uart(self, port: str):
        try:
            import serial  # type: ignore
            self._serial = serial.Serial(port, 9600, timeout=1)
            self._running = True
            self._thread = threading.Thread(target=self._uart_loop, daemon=True)
            self._thread.start()
        except Exception:
            pass  # UART not available; detection won't fire

    def _uart_loop(self):
        """Reads HuskyLens UART output. HuskyLens sends lines like: 'ID:3\n'"""
        while self._running and self._serial:
            try:
                line = self._serial.readline().decode("utf-8", errors="ignore").strip()
                if line.startswith("ID:"):
                    label_id = int(line[3:])
                    food = self._lookup_label(label_id)
                    if self._on_detection:
                        self._on_detection(food)
            except Exception:
                continue

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
        if self._serial:
            self._serial.close()
