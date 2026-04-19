import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "../components"

Item {
    id: root
    objectName: "Settings"
    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Flickable {
        anchors { fill: parent; margins: 24 }
        contentHeight: col.implicitHeight; clip: true

        Column {
            id: col
            width: parent.width; spacing: 20

            Text { text: "Settings"; font { pixelSize: 36; bold: true }
                   color: isDark ? "white" : "#0f172a" }

            // ── Test Mode ────────────────────────────────────────────────
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: testModeCol.implicitHeight + 24

                Column {
                    id: testModeCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 10

                    Item {
                        width: parent.width
                        height: Math.max(testLabelCol.height, testSwitch.height)
                        Column {
                            id: testLabelCol
                            anchors.left: parent.left
                            anchors.verticalCenter: parent.verticalCenter
                            spacing: 2
                            Text { text: "Test Mode"; font { pixelSize: 24; bold: true }
                                   color: isDark ? "white" : "#0f172a" }
                            Text { text: "Override HuskyLens + scale input"
                                   font.pixelSize: 18; color: muted }
                        }
                        Switch {
                            id: testSwitch
                            anchors.right: parent.right
                            anchors.verticalCenter: parent.verticalCenter
                            checked: appState.testMode
                            onToggled: appState.setTestMode(checked)
                        }
                    }

                    // Food picker (test mode only)
                    Column {
                        id: testPicker
                        width: parent.width; spacing: 6
                        visible: appState.testMode

                        // Staged selection — only pushed to AppState when the user
                        // taps "Scan selection" below, so they can tweak weight first.
                        // Defaults to 0 so the staged index matches what the
                        // ComboBox visually shows before the user taps anything.
                        property int selectedIndex: 0
                        property real selectedWeight: 0

                        Text { text: "SELECT FOOD"; font.pixelSize: 16; font.letterSpacing: 1; color: muted }
                        ComboBox {
                            width: parent.width
                            leftPadding: 12; rightPadding: 12
                            topPadding: 8;   bottomPadding: 8
                            model: appState.allFoods.map(f => f.name)
                            onActivated: testPicker.selectedIndex = currentIndex
                        }

                        Text { text: "WEIGHT (g)"; font.pixelSize: 16; font.letterSpacing: 1; color: muted }
                        TextField {
                            id: weightField
                            width: parent.width
                            leftPadding: 12; rightPadding: 12
                            topPadding: 10;  bottomPadding: 10
                            inputMethodHints: Qt.ImhFormattedNumbersOnly
                            placeholderText: "e.g. 182"
                            onTextChanged: testPicker.selectedWeight = parseFloat(text) || 0
                        }

                        // Confirm button — triggers the simulated scan
                        Rectangle {
                            width: parent.width; height: 60; radius: 10
                            anchors.topMargin: 4
                            property bool ready: testPicker.selectedIndex >= 0 &&
                                                 testPicker.selectedWeight > 0
                            gradient: Gradient {
                                orientation: Gradient.Horizontal
                                GradientStop { position: 0.0; color: parent.ready ? "#6366f1" : "#334155" }
                                GradientStop { position: 1.0; color: parent.ready ? "#06b6d4" : "#475569" }
                            }
                            Text {
                                anchors.centerIn: parent
                                text: "Scan selection"
                                color: "white"
                                font { pixelSize: 22; bold: true }
                            }
                            MouseArea {
                                anchors.fill: parent
                                enabled: parent.ready
                                onClicked: {
                                    appState.setTestWeight(testPicker.selectedWeight)
                                    appState.injectFood(
                                        appState.allFoods[testPicker.selectedIndex].id)
                                }
                            }
                        }
                    }
                }
            }

            // ── Theme ────────────────────────────────────────────────────
            Rectangle {
                width: parent.width; height: 80; radius: 12; color: surface
                Item {
                    anchors { fill: parent; margins: 12 }
                    Column {
                        spacing: 2; anchors.left: parent.left
                        anchors.verticalCenter: parent.verticalCenter
                        Text { text: "Theme"; font { pixelSize: 24; bold: true }
                               color: isDark ? "white" : "#0f172a" }
                        Text { text: "Dark Cosmos / Clean Light"
                               font.pixelSize: 18; color: muted }
                    }
                    Row {
                        spacing: 4
                        anchors.right: parent.right
                        anchors.verticalCenter: parent.verticalCenter
                        Repeater {
                            model: ["Dark", "Light"]
                            Rectangle {
                                width: 72; height: 42; radius: 8
                                color: (modelData === "Dark" && appState.theme === "dark") ||
                                       (modelData === "Light" && appState.theme === "light")
                                       ? "#6366f1" : (isDark ? "#0f172a" : "#f1f5f9")
                                Text { anchors.centerIn: parent; text: modelData
                                       font.pixelSize: 20
                                       color: (modelData === "Dark" && appState.theme === "dark") ||
                                              (modelData === "Light" && appState.theme === "light")
                                              ? "white" : muted }
                                MouseArea {
                                    anchors.fill: parent
                                    onClicked: appState.setTheme(modelData.toLowerCase())
                                }
                            }
                        }
                    }
                }
            }

            // ── HuskyLens Label Mapping ────────────────────────────────
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: mappingCol.implicitHeight + 24

                Column {
                    id: mappingCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 8

                    Text { text: "HuskyLens Label Mapping"
                           font { pixelSize: 24; bold: true }
                           color: isDark ? "white" : "#0f172a" }

                    Repeater {
                        model: appState.allFoods
                                .filter(f => f.huskylens_label_id !== null)
                                .sort((a, b) => a.huskylens_label_id - b.huskylens_label_id)

                        Rectangle {
                            width: parent.width; height: 52; radius: 8
                            color: isDark ? "#0f172a" : "#f8fafc"
                            Item {
                                anchors { fill: parent; leftMargin: 10; rightMargin: 10 }
                                Text { anchors.left: parent.left
                                       anchors.verticalCenter: parent.verticalCenter
                                       text: "Label ID " + modelData.huskylens_label_id
                                       font.pixelSize: 20; color: muted }
                                Text { anchors.right: parent.right
                                       anchors.verticalCenter: parent.verticalCenter
                                       text: modelData.name
                                       font { pixelSize: 22; bold: true }
                                       color: isDark ? "white" : "#0f172a" }
                            }
                        }
                    }
                }
            }

            // ── Paired Users ─────────────────────────────────────────────
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: usersCol.implicitHeight + 24

                Column {
                    id: usersCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 8

                    Text { text: "Paired Users"
                           font { pixelSize: 24; bold: true }
                           color: isDark ? "white" : "#0f172a" }

                    Rectangle {
                        width: parent.width; height: 60; radius: 8
                        color: isDark ? "#0f172a" : "#f8fafc"
                        visible: appState.userConnected

                        Row {
                            anchors { fill: parent; leftMargin: 14; rightMargin: 14 }
                            spacing: 12
                            Rectangle {
                                width: 36; height: 36; radius: 18
                                anchors.verticalCenter: parent.verticalCenter
                                gradient: Gradient {
                                    orientation: Gradient.Horizontal
                                    GradientStop { position: 0.0; color: "#6366f1" }
                                    GradientStop { position: 1.0; color: "#06b6d4" }
                                }
                                Text { anchors.centerIn: parent
                                       text: appState.activeUserName[0].toUpperCase()
                                       font.pixelSize: 18; font.bold: true; color: "white" }
                            }
                            Text { anchors.verticalCenter: parent.verticalCenter
                                   text: appState.activeUserName
                                   font.pixelSize: 22; color: isDark ? "white" : "#0f172a" }
                            Item { Layout.fillWidth: true; width: 1 }
                            Row {
                                spacing: 4; anchors.verticalCenter: parent.verticalCenter
                                Rectangle { width: 6; height: 6; radius: 3; color: "#22c55e" }
                                Text { text: "Active"; font.pixelSize: 18; color: muted }
                            }
                        }
                    }

                    Text {
                        visible: !appState.userConnected
                        text: "No device connected."
                        font.pixelSize: 20; color: muted
                    }
                }
            }
        }
    }
}
