import subprocess
import os

from PyQt6.QtCore import QObject, pyqtSignal, pyqtSlot, pyqtProperty, QTimer, QCoreApplication
from services.calorie_calculator import calculate_calories, calculate_daily_goal
from services.food_detection import FoodDetectionService
from services.weight import WeightService
from services.user_session import UserSessionManager, GUEST_MAC
from services.bluetooth_server import BluetoothServer
from database.db import get_connection, run_migrations
from database.seed_foods import seed_foods


class AppState(QObject):
    # Signals to QML
    weightChanged       = pyqtSignal(float)
    scanDetected        = pyqtSignal('QVariantMap')   # food dict
    logUpdated          = pyqtSignal()
    userChanged         = pyqtSignal()
    testModeChanged     = pyqtSignal(bool)
    themeChanged        = pyqtSignal(str)             # 'dark' | 'light'
    bleStatusChanged    = pyqtSignal(bool)

    def __init__(self, test_mode: bool = False, parent=None):
        super().__init__(parent)
        self._test_mode = test_mode
        self._theme = "dark"
        self._last_scan_kcal: float = 0.0

        # DB
        self._conn = get_connection()
        run_migrations(self._conn)
        seed_foods(self._conn)

        # Services
        self._session = UserSessionManager(self._conn)
        self._weight_svc = WeightService(test_mode=test_mode)
        self._detection_svc = FoodDetectionService(
            conn=self._conn,
            test_mode=test_mode,
            on_detection=self._on_food_detected,
        )
        self._ble = BluetoothServer(
            on_profile_received=self._on_profile_received,
            on_health_received=self._on_health_received,
            enabled=not test_mode,
        )

        # Active user (guest until phone connects)
        self._active_user = self._session.upsert_user(GUEST_MAC, {
            "name": None, "age": None, "weight_kg": None, "height_cm": None,
            "sex": "other", "activity_level": "sedentary", "daily_calorie_goal": None,
        })
        self._last_scan: dict = {}
        self._cal_raw_point: float = 0.0
        self._cal_known_grams: float = 0.0

        # Poll weight every 100 ms
        self._weight_timer = QTimer(self)
        self._weight_timer.timeout.connect(self._poll_weight)
        self._weight_timer.start(100)

    # ── Weight ──────────────────────────────────────────────────────────────

    def _poll_weight(self):
        w = self._weight_svc.read()
        self.weightChanged.emit(w)

    @pyqtProperty(float, notify=weightChanged)
    def currentWeightG(self):
        return self._weight_svc.current_weight_g

    @pyqtProperty(bool)
    def weightIsStable(self):
        return self._weight_svc.is_stable

    # ── Food Detection ───────────────────────────────────────────────────────

    def _on_food_detected(self, food: dict | None):
        if food is None:
            return
        self._last_scan = food
        self.scanDetected.emit(food)

    @pyqtSlot(int)
    def injectFood(self, food_id: int):
        """Test mode: simulate a HuskyLens detection."""
        self._detection_svc.inject_food_by_id(food_id)

    @pyqtSlot(float)
    def setTestWeight(self, grams: float):
        """Test mode: override scale reading."""
        self._weight_svc.set_test_weight(grams)
        self.weightChanged.emit(grams)

    @pyqtProperty(list, constant=True)
    def allFoods(self):
        return self._detection_svc.get_all_foods()

    # ── Calorie Calculations ─────────────────────────────────────────────────

    @pyqtSlot(float, float, result=float)
    def calculateCalories(self, calories_per_100g: float, weight_g: float) -> float:
        return calculate_calories(calories_per_100g, weight_g)

    @pyqtProperty(float, notify=logUpdated)
    def totalCaloriesToday(self):
        return self._session.total_calories_today(self._active_user["id"])

    @pyqtProperty(float, notify=userChanged)
    def dailyGoal(self):
        from types import SimpleNamespace
        u = SimpleNamespace(**self._active_user)
        return calculate_daily_goal(u)

    @pyqtProperty(float, notify=logUpdated)
    def remainingCalories(self):
        return max(0.0, self.dailyGoal - self.totalCaloriesToday)

    # ── Food Log ─────────────────────────────────────────────────────────────

    @pyqtSlot(int, float, float)
    def confirmScan(self, food_id: int, weight_g: float, calories: float):
        self._session.log_food(self._active_user["id"], food_id, weight_g, calories)
        self._last_scan_kcal = calories
        self.logUpdated.emit()
        self._push_state_to_phone()

    @pyqtSlot(int)
    def deleteLogEntry(self, entry_id: int):
        self._session.delete_log_entry(entry_id)
        self.logUpdated.emit()
        self._push_state_to_phone()

    @pyqtSlot()
    def refreshHome(self):
        """Dashboard refresh button: re-emit log/user so QML re-binds, and
        re-broadcast state to any connected phone."""
        self.logUpdated.emit()
        self.userChanged.emit()
        self._push_state_to_phone()

    def _push_state_to_phone(self):
        """Send today's log + session state over BLE to the connected phone.

        No-op when the active session is the guest (no phone paired).
        """
        from services.user_session import GUEST_MAC
        device_id = self._active_user.get("bluetooth_mac")
        if not device_id or device_id == GUEST_MAC:
            return
        self._sync_to_client(device_id)

    @pyqtProperty(list, notify=logUpdated)
    def todaysLog(self):
        return self._session.get_todays_log(self._active_user["id"])

    # ── Activity / Health ────────────────────────────────────────────────────

    @pyqtProperty('QVariantMap', notify=userChanged)
    def healthSnapshot(self):
        snap = self._session.get_health_snapshot(self._active_user["id"])
        return snap or {"steps": 0, "calories_burned": 0, "active_minutes": 0, "workouts": 0}

    # ── BLE Callbacks ────────────────────────────────────────────────────────

    def _on_profile_received(self, device_id: str, profile: dict):
        self._active_user = self._session.upsert_user(device_id, profile)
        self.userChanged.emit()
        self._sync_to_client(device_id)

    def _on_health_received(self, device_id: str, snapshot: dict):
        self._session.update_health_snapshot(self._active_user["id"], snapshot)
        self.userChanged.emit()
        self._sync_to_client(device_id)

    def _sync_to_client(self, device_id: str):
        log = self._session.get_todays_log(self._active_user["id"])
        self._ble.notify_food_log(device_id, log)
        self._ble.notify_session_state(device_id, {
            "calories_consumed": self.totalCaloriesToday,
            "calories_remaining": self.remainingCalories,
            "last_scan_food": self._last_scan.get("name", ""),
            "last_scan_kcal": self._last_scan_kcal,
        })

    # ── Theme / Test Mode ────────────────────────────────────────────────────

    @pyqtProperty(bool, notify=testModeChanged)
    def testMode(self):
        return self._test_mode

    @pyqtSlot(bool)
    def setTestMode(self, enabled: bool):
        self._test_mode = enabled
        self._weight_svc._test_mode = enabled
        self._detection_svc._test_mode = enabled
        self.testModeChanged.emit(enabled)

    @pyqtProperty(str, notify=themeChanged)
    def theme(self):
        return self._theme

    @pyqtSlot(str)
    def setTheme(self, theme: str):
        self._theme = theme
        self.themeChanged.emit(theme)

    @pyqtProperty(str, notify=userChanged)
    def activeUserName(self):
        return self._active_user.get("name") or "Guest"

    @pyqtProperty(bool, notify=userChanged)
    def userConnected(self):
        from services.user_session import GUEST_MAC
        return self._active_user.get("bluetooth_mac") != GUEST_MAC

    @pyqtProperty(bool, constant=True)
    def bleAvailable(self) -> bool:
        """True when the real BLE GATT peripheral is advertising.

        QML uses this to decide whether to offer the real "open app on your
        phone" prompt or the simulated-pairing shortcut.
        """
        return bool(getattr(self._ble, "started", False))

    # ── Simulated pairing (dev / fallback) ───────────────────────────────────
    # The real BLE GATT peripheral lives in services/bluetooth_server.py.
    # When that peripheral isn't advertising (test mode, bless import failed,
    # or macOS refusing Bluetooth permission), tapping "Connect phone" has
    # nothing to bind to. This slot mocks the payload an iOS client would
    # have written so the UI can be exercised end-to-end without a phone.
    @pyqtSlot()
    def simulatePhonePairing(self):
        from datetime import date
        mock_device_id = "SIMULATED:00:11:22:33:44:55"
        mock_profile = {
            "name": "Test User",
            "age": 28,
            "weight_kg": 72.0,
            "height_cm": 178.0,
            "sex": "male",
            "activity_level": "moderate",
            "daily_calorie_goal": None,
        }
        mock_health = {
            "date": date.today().isoformat(),
            "steps": 6420,
            "calories_burned": 312,
            "active_minutes": 34,
            "workouts": 1,
        }
        self._on_profile_received(mock_device_id, mock_profile)
        self._on_health_received(mock_device_id, mock_health)

    # ── Admin Actions ────────────────────────────────────────────────────────

    @pyqtSlot()
    def stopService(self):
        subprocess.run(["sudo", "systemctl", "stop", "antidonut-kiosk"], check=False)
        QCoreApplication.quit()

    @pyqtSlot()
    def updateAndRestart(self):
        project_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        subprocess.run(["git", "-C", project_dir, "pull"], check=False)
        subprocess.run(["sudo", "systemctl", "restart", "antidonut-kiosk"], check=False)

    @pyqtSlot(result=float)
    def calibrateTare(self) -> float:
        self._weight_svc.tare()
        return self._weight_svc.read_raw()

    @pyqtSlot(float, result=float)
    def calibratePoint(self, known_grams: float) -> float:
        raw = self._weight_svc.read_raw()
        self._cal_raw_point = raw
        self._cal_known_grams = known_grams
        return raw

    @pyqtSlot(result=bool)
    def finalizeCalibration(self) -> bool:
        try:
            ref_unit = self._weight_svc.compute_calibration(
                self._cal_raw_point, self._cal_known_grams
            )
            self._weight_svc.save_calibration(ref_unit)
        except ValueError:
            return False
        return True
