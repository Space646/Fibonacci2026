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
            property string rawReadings: ""

            Text {
                width: parent.width
                wrapMode: Text.WordWrap
                font.pixelSize: 14
                color: "#f87171"
                visible: text !== ""
                text: appState.scaleHardwareError()
            }

            Text {
                width: parent.width
                wrapMode: Text.WordWrap
                font.pixelSize: 20
                color: "white"
                text: parent.step === 1 ? "Step 1: Remove everything from the scale, then tap Tare."
                    : parent.step === 2 ? "Step 2: Place a known weight on the scale and enter its weight below."
                    : parent.statusText
            }

            Text {
                width: parent.width
                wrapMode: Text.WordWrap
                font.pixelSize: 16
                color: "#94a3b8"
                visible: parent.rawReadings !== ""
                text: parent.rawReadings
            }

            TextField {
                id: calWeightField
                width: parent.width
                visible: parent.step === 2
                leftPadding: 12; rightPadding: 12; topPadding: 10; bottomPadding: 10
                inputMethodHints: Qt.ImhFormattedNumbersOnly
                placeholderText: "Known weight (g)"
                color: "white"
            }

            Rectangle {
                width: parent.width; height: 60; radius: 10
                visible: parent.step <= 2
                gradient: Gradient {
                    orientation: Gradient.Horizontal
                    GradientStop { position: 0.0; color: "#6366f1" }
                    GradientStop { position: 1.0; color: "#06b6d4" }
                }
                Text {
                    anchors.centerIn: parent
                    text: parent.parent.step === 1 ? "Tare" : "Confirm"
                    color: "white"; font { pixelSize: 22; bold: true }
                }
                MouseArea {
                    anchors.fill: parent
                    onClicked: {
                        var col = parent.parent
                        if (col.step === 1) {
                            var rawAfterTare = appState.calibrateTare()
                            col.rawReadings = "Raw after tare: " + rawAfterTare.toFixed(1)
                            col.step = 2
                        } else if (col.step === 2) {
                            var w = parseFloat(calWeightField.text) || 0
                            if (w > 0) {
                                var rawPt = appState.calibratePoint(w)
                                col.rawReadings += "  |  Raw point: " + rawPt.toFixed(1)
                                var ok = appState.finalizeCalibration()
                                col.step = 3
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
                    text: parent.parent.step === 3 ? "Done" : "Cancel"
                    color: "#94a3b8"; font { pixelSize: 20; bold: true }
                }
                MouseArea { anchors.fill: parent; onClicked: overlay.view = "menu" }
            }
        }
    }
}
