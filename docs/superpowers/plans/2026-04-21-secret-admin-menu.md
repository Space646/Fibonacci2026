# Secret Admin Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the visible Test Mode UI with a hidden admin menu triggered by long-pressing the refresh button.

**Architecture:** A full-screen QML overlay component (`AdminOverlay.qml`) manages four admin actions. Backend slots in `AppState` handle system operations (stop service, git pull, restart) and calibration. `WeightService` gains calibration persistence via a JSON file.

**Tech Stack:** Python 3.14, PyQt6, QML, HX711 (load cell), systemd, git

---

## File Structure

| Path | Action | Responsibility |
|------|--------|---------------|
| `pi-app/ui/components/AdminOverlay.qml` | Create | Full overlay UI: menu, food picker, calibration wizard |
| `pi-app/ui/screens/Dashboard.qml` | Modify | Add long-press timer to refresh button |
| `pi-app/ui/screens/Settings.qml` | Modify | Remove Test Mode section |
| `pi-app/ui/main.qml` | Modify | Remove test banner, add AdminOverlay instance |
| `pi-app/ui/app_state.py` | Modify | Add admin slots (stop, update, calibrate) |
| `pi-app/services/weight.py` | Modify | Add calibration loading/saving, `read_raw()` |
| `pi-app/config/scale_calibration.json` | Created at runtime | Calibration data |
| `pi-app/tests/test_weight.py` | Modify | Add calibration tests |
| `pi-app/tests/test_admin_actions.py` | Create | Test admin backend slots |

---

### Task 1: WeightService Calibration Support

**Files:**
- Modify: `pi-app/services/weight.py`
- Modify: `pi-app/tests/test_weight.py`

- [ ] **Step 1: Write failing tests for calibration**

Add to `pi-app/tests/test_weight.py`:

```python
import json
import os
import tempfile
from services.weight import WeightService


def test_load_calibration_from_json(tmp_path):
    cal_file = tmp_path / "scale_calibration.json"
    cal_file.write_text(json.dumps({
        "offset": 100.0,
        "scale_factor": 2.0,
        "calibrated_at": "2026-04-21T12:00:00"
    }))
    svc = WeightService(test_mode=False, skip_hardware_init=True,
                        calibration_file=str(cal_file))
    assert svc._offset == 100.0
    assert svc._scale_factor == 2.0


def test_load_calibration_missing_file():
    svc = WeightService(test_mode=False, skip_hardware_init=True,
                        calibration_file="/nonexistent/path.json")
    assert svc._offset == 0.0
    assert svc._scale_factor == 1.0


def test_read_raw_bypasses_calibration():
    svc = WeightService(test_mode=True)
    svc._offset = 50.0
    svc._scale_factor = 2.0
    svc.set_test_weight(200.0)
    assert svc.read_raw() == 200.0


def test_read_applies_calibration():
    svc = WeightService(test_mode=True)
    svc._offset = 50.0
    svc._scale_factor = 2.0
    svc._test_weight = 150.0
    reading = svc.read()
    assert reading == (150.0 - 50.0) / 2.0


def test_save_calibration(tmp_path):
    cal_file = tmp_path / "scale_calibration.json"
    svc = WeightService(test_mode=True, calibration_file=str(cal_file))
    svc.save_calibration(offset=100.0, scale_factor=2.5)
    data = json.loads(cal_file.read_text())
    assert data["offset"] == 100.0
    assert data["scale_factor"] == 2.5
    assert "calibrated_at" in data


def test_compute_calibration_from_points():
    svc = WeightService(test_mode=True)
    offset, scale_factor = svc.compute_calibration(
        raw_zero=100.0,
        raw_point1=300.0, known_weight1=100.0,
        raw_point2=500.0, known_weight2=200.0,
    )
    assert offset == 100.0
    assert scale_factor == 2.0
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd pi-app && python -m pytest tests/test_weight.py -v`
Expected: FAIL — `read_raw` not defined, `calibration_file` param not recognized, etc.

- [ ] **Step 3: Implement calibration in WeightService**

Replace `pi-app/services/weight.py` with:

```python
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
            if self._scale_factor == 1.0 and self._offset == 0.0:
                return self._test_weight
            raw = self._test_weight
            reading = max(0.0, round((raw - self._offset) / self._scale_factor, 1))
            return reading

        if self._hx is None:
            return self._test_weight

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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd pi-app && python -m pytest tests/test_weight.py -v`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add pi-app/services/weight.py pi-app/tests/test_weight.py
git commit -m "feat(weight): add calibration loading, saving, and computation"
```

---

### Task 2: Admin Backend Slots in AppState

**Files:**
- Modify: `pi-app/ui/app_state.py`
- Create: `pi-app/tests/test_admin_actions.py`

- [ ] **Step 1: Write failing tests for admin actions**

Create `pi-app/tests/test_admin_actions.py`:

```python
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd pi-app && python -m pytest tests/test_admin_actions.py -v`
Expected: FAIL — `stopService`, `calibrateTare`, etc. not defined

- [ ] **Step 3: Add admin slots to AppState**

Add these imports at the top of `pi-app/ui/app_state.py`:

```python
import subprocess
import os
```

Add these methods to the `AppState` class (after the Theme / Test Mode section):

```python
    # ── Admin Actions ────────────────────────────────────────────────────────

    _cal_raw_zero: float = 0.0
    _cal_points: list = []

    @pyqtSlot()
    def stopService(self):
        subprocess.run(["systemctl", "stop", "fibonacci-health.service"], check=False)

    @pyqtSlot()
    def updateAndRestart(self):
        project_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        subprocess.run(["git", "-C", project_dir, "pull"], check=False)
        subprocess.run(["systemctl", "restart", "fibonacci-health.service"], check=False)

    @pyqtSlot()
    def calibrateTare(self):
        self._cal_raw_zero = self._weight_svc.read_raw()
        self._cal_points = []

    @pyqtSlot(float)
    def calibratePoint(self, known_grams: float):
        raw = self._weight_svc.read_raw()
        self._cal_points.append((raw, known_grams))

    @pyqtSlot(result=bool)
    def finalizeCalibration(self) -> bool:
        if len(self._cal_points) < 2:
            return False
        raw1, weight1 = self._cal_points[0]
        raw2, weight2 = self._cal_points[1]
        offset, scale_factor = self._weight_svc.compute_calibration(
            raw_zero=self._cal_raw_zero,
            raw_point1=raw1, known_weight1=weight1,
            raw_point2=raw2, known_weight2=weight2,
        )
        self._weight_svc.save_calibration(offset, scale_factor)
        return True
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd pi-app && python -m pytest tests/test_admin_actions.py -v`
Expected: All PASS

- [ ] **Step 5: Commit**

```bash
git add pi-app/ui/app_state.py pi-app/tests/test_admin_actions.py
git commit -m "feat(admin): add backend slots for stop, update, calibrate"
```

---

### Task 3: Remove Test Mode from Settings.qml

**Files:**
- Modify: `pi-app/ui/screens/Settings.qml`

- [ ] **Step 1: Remove the Test Mode section**

In `pi-app/ui/screens/Settings.qml`, delete lines 24–118 (the entire `// ── Test Mode` section including the `Rectangle` that wraps `testModeCol`). The file should go directly from the `spacing: 20` line in the column to the `// ── Theme` section.

The resulting file after the header `Column` opening should be:

```qml
            Text { text: "Settings"; font { pixelSize: 36; bold: true }
                   color: isDark ? "white" : "#0f172a" }

            // ── Theme ────────────────────────────────────────────────────
            Rectangle {
```

(Everything from `// ── Theme` onward stays unchanged.)

- [ ] **Step 2: Verify the file is valid QML (visual check)**

Open the file and confirm:
- No dangling references to `testModeCol`, `testSwitch`, `testPicker`, `weightField`
- `import "../components"` can be removed since no components from that dir are used (check — if `HuskyLens Label Mapping` or others use them, keep it)

- [ ] **Step 3: Commit**

```bash
git add pi-app/ui/screens/Settings.qml
git commit -m "refactor(settings): remove Test Mode UI section"
```

---

### Task 4: Remove Test Banner from main.qml

**Files:**
- Modify: `pi-app/ui/main.qml`

- [ ] **Step 1: Remove the test banner Rectangle**

Delete lines 14–28 (the `Rectangle { id: testBanner ... }` block).

- [ ] **Step 2: Fix StackView anchors**

Change the StackView's `anchors.top` from:

```qml
        anchors {
            top: appState.testMode ? testBanner.bottom : parent.top
            left: parent.left; right: parent.right; bottom: navBar.top
        }
```

to:

```qml
        anchors {
            top: parent.top
            left: parent.left; right: parent.right; bottom: navBar.top
        }
```

- [ ] **Step 3: Commit**

```bash
git add pi-app/ui/main.qml
git commit -m "refactor(main): remove test mode banner"
```

---

### Task 5: Create AdminOverlay.qml

**Files:**
- Create: `pi-app/ui/components/AdminOverlay.qml`

- [ ] **Step 1: Create the AdminOverlay component**

Create `pi-app/ui/components/AdminOverlay.qml`:

```qml
import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15

Rectangle {
    id: overlay
    anchors.fill: parent
    visible: false
    color: "#D9000000"
    z: 200

    property string view: "menu"

    function open() { view = "menu"; visible = true }
    function close() { visible = false }

    MouseArea { anchors.fill: parent; onClicked: {} }

    Rectangle {
        id: card
        anchors.centerIn: parent
        width: Math.min(parent.width - 48, 600)
        height: contentLoader.item ? Math.min(contentLoader.item.implicitHeight + 80, parent.height - 80) : 400
        radius: 16
        color: "#1e293b"

        Text {
            id: titleText
            anchors { top: parent.top; left: parent.left; margins: 20 }
            text: overlay.view === "menu" ? "Admin Menu"
                : overlay.view === "food" ? "Add Food Item"
                : overlay.view === "calibrate" ? "Calibrate Scale"
                : "Admin Menu"
            font { pixelSize: 28; bold: true }
            color: "white"
        }

        Rectangle {
            id: closeBtn
            anchors { top: parent.top; right: parent.right; margins: 12 }
            width: 40; height: 40; radius: 20
            color: "#334155"
            Text { anchors.centerIn: parent; text: "\u2715"; color: "#94a3b8"; font.pixelSize: 22 }
            MouseArea { anchors.fill: parent; onClicked: overlay.close() }
        }

        Loader {
            id: contentLoader
            anchors { top: titleText.bottom; left: parent.left; right: parent.right; bottom: parent.bottom; margins: 20 }
            sourceComponent: overlay.view === "menu" ? menuView
                           : overlay.view === "food" ? foodView
                           : overlay.view === "calibrate" ? calibrateView
                           : menuView
        }
    }

    Component {
        id: menuView
        Column {
            spacing: 12
            width: parent ? parent.width : 0

            Repeater {
                model: [
                    { label: "Close App", action: "stop" },
                    { label: "Add Food Item", action: "food" },
                    { label: "Update & Restart", action: "update" },
                    { label: "Calibrate Scale", action: "calibrate" }
                ]

                Rectangle {
                    width: parent.width; height: 60; radius: 10
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: "#6366f1" }
                        GradientStop { position: 1.0; color: "#06b6d4" }
                    }
                    Text {
                        anchors.centerIn: parent
                        text: modelData.label
                        color: "white"
                        font { pixelSize: 22; bold: true }
                    }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: {
                            if (modelData.action === "stop") {
                                appState.stopService()
                            } else if (modelData.action === "food") {
                                overlay.view = "food"
                            } else if (modelData.action === "update") {
                                appState.updateAndRestart()
                            } else if (modelData.action === "calibrate") {
                                overlay.view = "calibrate"
                            }
                        }
                    }
                }
            }
        }
    }

    Component {
        id: foodView
        Column {
            spacing: 12
            width: parent ? parent.width : 0

            property int selectedIndex: 0
            property real selectedWeight: 0

            Text { text: "SELECT FOOD"; font.pixelSize: 16; font.letterSpacing: 1; color: "#64748b" }
            ComboBox {
                width: parent.width
                leftPadding: 12; rightPadding: 12; topPadding: 8; bottomPadding: 8
                model: appState.allFoods.map(f => f.name)
                onActivated: parent.selectedIndex = currentIndex
            }

            Text { text: "WEIGHT (g)"; font.pixelSize: 16; font.letterSpacing: 1; color: "#64748b" }
            TextField {
                width: parent.width
                leftPadding: 12; rightPadding: 12; topPadding: 10; bottomPadding: 10
                inputMethodHints: Qt.ImhFormattedNumbersOnly
                placeholderText: "e.g. 182"
                color: "white"
                onTextChanged: parent.selectedWeight = parseFloat(text) || 0
            }

            Rectangle {
                width: parent.width; height: 60; radius: 10
                property bool ready: parent.selectedIndex >= 0 && parent.selectedWeight > 0
                gradient: Gradient {
                    orientation: Gradient.Horizontal
                    GradientStop { position: 0.0; color: parent.ready ? "#6366f1" : "#334155" }
                    GradientStop { position: 1.0; color: parent.ready ? "#06b6d4" : "#475569" }
                }
                Text { anchors.centerIn: parent; text: "Confirm"; color: "white"; font { pixelSize: 22; bold: true } }
                MouseArea {
                    anchors.fill: parent
                    enabled: parent.ready
                    onClicked: {
                        appState.setTestWeight(parent.parent.selectedWeight)
                        appState.injectFood(appState.allFoods[parent.parent.selectedIndex].id)
                        overlay.close()
                    }
                }
            }

            Rectangle {
                width: parent.width; height: 50; radius: 10; color: "#334155"
                Text { anchors.centerIn: parent; text: "Back"; color: "#94a3b8"; font { pixelSize: 20; bold: true } }
                MouseArea { anchors.fill: parent; onClicked: overlay.view = "menu" }
            }
        }
    }

    Component {
        id: calibrateView
        Column {
            spacing: 12
            width: parent ? parent.width : 0

            property int step: 1
            property string statusText: ""

            Text {
                width: parent.width
                wrapMode: Text.WordWrap
                font.pixelSize: 20
                color: "white"
                text: parent.step === 1 ? "Step 1: Remove everything from the scale, then tap Next."
                    : parent.step === 2 ? "Step 2: Place a known weight on the scale and enter its weight below."
                    : parent.step === 3 ? "Step 3: Add a second item (keep the first). Enter the combined weight below."
                    : parent.statusText
            }

            TextField {
                id: calWeightField
                width: parent.width
                visible: parent.step === 2 || parent.step === 3
                leftPadding: 12; rightPadding: 12; topPadding: 10; bottomPadding: 10
                inputMethodHints: Qt.ImhFormattedNumbersOnly
                placeholderText: parent.step === 2 ? "Known weight (g)" : "Combined weight (g)"
                color: "white"
            }

            Rectangle {
                width: parent.width; height: 60; radius: 10
                visible: parent.step <= 3
                gradient: Gradient {
                    orientation: Gradient.Horizontal
                    GradientStop { position: 0.0; color: "#6366f1" }
                    GradientStop { position: 1.0; color: "#06b6d4" }
                }
                Text {
                    anchors.centerIn: parent
                    text: parent.parent.step === 1 ? "Next" : "Confirm"
                    color: "white"; font { pixelSize: 22; bold: true }
                }
                MouseArea {
                    anchors.fill: parent
                    onClicked: {
                        var col = parent.parent
                        if (col.step === 1) {
                            appState.calibrateTare()
                            col.step = 2
                        } else if (col.step === 2) {
                            var w1 = parseFloat(calWeightField.text) || 0
                            if (w1 > 0) {
                                appState.calibratePoint(w1)
                                calWeightField.text = ""
                                col.step = 3
                            }
                        } else if (col.step === 3) {
                            var w2 = parseFloat(calWeightField.text) || 0
                            if (w2 > 0) {
                                appState.calibratePoint(w2)
                                var ok = appState.finalizeCalibration()
                                col.step = 4
                                col.statusText = ok ? "Calibration complete!" : "Calibration failed. Try again."
                            }
                        }
                    }
                }
            }

            Rectangle {
                width: parent.width; height: 50; radius: 10; color: "#334155"
                Text {
                    anchors.centerIn: parent
                    text: parent.parent.step === 4 ? "Done" : "Cancel"
                    color: "#94a3b8"; font { pixelSize: 20; bold: true }
                }
                MouseArea { anchors.fill: parent; onClicked: overlay.view = "menu" }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add pi-app/ui/components/AdminOverlay.qml
git commit -m "feat(admin): create AdminOverlay QML component"
```

---

### Task 6: Add AdminOverlay to main.qml

**Files:**
- Modify: `pi-app/ui/main.qml`

- [ ] **Step 1: Add the AdminOverlay instance**

In `pi-app/ui/main.qml`, after the `StackView` closing brace and before the `// Bottom navigation bar` comment, add:

```qml
    // Admin overlay (triggered by long-press on Dashboard refresh button)
    AdminOverlay {
        id: adminOverlay
        anchors.fill: parent
    }
```

Also add `import "components"` at the top if not already present. The existing `import QtQuick 2.15` etc. lines stay; add this line after them:

```qml
import "components"
```

- [ ] **Step 2: Expose a function to open the overlay from Dashboard**

Add a function on the root `ApplicationWindow` so Dashboard can call it:

```qml
    function openAdminMenu() { adminOverlay.open() }
```

Place this inside `ApplicationWindow` before the `StackView`.

- [ ] **Step 3: Commit**

```bash
git add pi-app/ui/main.qml
git commit -m "feat(admin): wire AdminOverlay into main window"
```

---

### Task 7: Long-Press Trigger on Dashboard Refresh Button

**Files:**
- Modify: `pi-app/ui/screens/Dashboard.qml`

- [ ] **Step 1: Replace the MouseArea on the refresh button**

In `pi-app/ui/screens/Dashboard.qml`, find the `MouseArea` inside `refreshBtn` (around line 101–108). Replace it with a long-press-aware version:

```qml
                    MouseArea {
                        anchors.fill: parent
                        property bool longPressTriggered: false

                        onPressed: {
                            longPressTriggered = false
                            adminTimer.restart()
                        }
                        onReleased: {
                            adminTimer.stop()
                            if (!longPressTriggered) {
                                refreshBtn.scale = 0.88
                                appState.refreshHome()
                                spinReset.restart()
                            }
                        }
                        onCanceled: {
                            adminTimer.stop()
                        }

                        Timer {
                            id: adminTimer
                            interval: 5000
                            repeat: false
                            onTriggered: {
                                parent.longPressTriggered = true
                                root.ApplicationWindow.window.openAdminMenu()
                            }
                        }
                    }
```

- [ ] **Step 2: Verify QML structure**

Confirm the `spinReset` Timer and `Behavior on scale` blocks are still siblings (not children) of the new MouseArea. They should remain at the same level as before, inside `refreshBtn`.

- [ ] **Step 3: Commit**

```bash
git add pi-app/ui/screens/Dashboard.qml
git commit -m "feat(admin): add 5-second long-press trigger on refresh button"
```

---

### Task 8: Integration Verification

**Files:** (no new files)

- [ ] **Step 1: Run full test suite**

Run: `cd pi-app && python -m pytest tests/ -v`
Expected: All tests PASS

- [ ] **Step 2: Verify app launches**

Run: `cd pi-app && python main.py --test`
Expected: App launches without errors, no test banner visible, Settings page shows Theme/HuskyLens/Users but no Test Mode

- [ ] **Step 3: Verify admin overlay opens**

Long-press the refresh button for 5 seconds. Admin overlay should appear.

- [ ] **Step 4: Commit any fixes if needed**

```bash
git add -A && git commit -m "fix: integration fixes for admin menu"
```
