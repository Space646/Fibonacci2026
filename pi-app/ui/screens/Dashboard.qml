import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "../components"

Item {
    id: root
    objectName: "Home"

    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Flickable {
        anchors { fill: parent; margins: 24 }
        contentHeight: col.implicitHeight
        clip: true

        Column {
            id: col
            width: parent.width
            spacing: 20

            // Header
            RowLayout {
                width: parent.width
                spacing: 14

                Rectangle {
                    Layout.preferredWidth: 56; Layout.preferredHeight: 56
                    radius: 28
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: "#6366f1" }
                        GradientStop { position: 1.0; color: "#06b6d4" }
                    }
                    Text {
                        anchors.centerIn: parent
                        text: appState.activeUserName.length > 0
                              ? appState.activeUserName[0].toUpperCase() : "G"
                        color: "white"; font { pixelSize: 28; bold: true }
                    }
                }

                Column {
                    Layout.alignment: Qt.AlignVCenter
                    spacing: 2
                    Text {
                        text: appState.activeUserName
                        color: isDark ? "white" : "#0f172a"
                        font { pixelSize: 26; bold: true }
                    }
                    Text {
                        text: Qt.formatDate(new Date(), "dddd d MMM")
                        color: muted; font.pixelSize: 18
                    }
                }

                Item { Layout.fillWidth: true }

                // Connect prompt when no user.
                //  • BLE advertising  → static hint telling the user to open
                //    the iOS app. The Pi is the peripheral, it cannot
                //    initiate pairing, so tapping should be a no-op here.
                //  • BLE not running  → dev shortcut: simulate a phone
                //    pairing so the UI can be exercised.
                Rectangle {
                    visible: !appState.userConnected
                    color: surface; radius: 8
                    Layout.preferredHeight: 42
                    Layout.preferredWidth: connectText.implicitWidth + 20
                    Layout.alignment: Qt.AlignVCenter
                    Text {
                        id: connectText
                        anchors.centerIn: parent
                        text: appState.bleAvailable
                              ? "Open FiboHealth on your phone"
                              : "Simulate phone"
                        color: "#6366f1"; font.pixelSize: 18
                    }
                    MouseArea {
                        anchors.fill: parent
                        enabled: !appState.bleAvailable
                        onClicked: appState.simulatePhonePairing()
                    }
                }

                // Refresh button: reload today's log + re-broadcast state
                // to any connected phone.
                Rectangle {
                    id: refreshBtn
                    Layout.preferredWidth: 42; Layout.preferredHeight: 42
                    Layout.alignment: Qt.AlignVCenter
                    radius: 21
                    color: surface
                    Text {
                        anchors.centerIn: parent
                        text: "\u21bb"            // ↻
                        color: "#6366f1"
                        font { pixelSize: 28; bold: true }
                    }
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
                    Timer {
                        id: spinReset
                        interval: 120
                        onTriggered: refreshBtn.scale = 1.0
                    }
                    Behavior on scale {
                        NumberAnimation { duration: 120; easing.type: Easing.OutQuad }
                    }
                }
            }

            // Calorie ring
            CalorieRing {
                anchors.horizontalCenter: parent.horizontalCenter
                width: 240; height: 240
                consumed: appState.totalCaloriesToday
                goal: appState.dailyGoal
                isDark: root.isDark
            }

            // Stats row
            Row {
                width: parent.width
                spacing: 12
                StatCard {
                    width: (parent.width - 12) / 2
                    value: Math.round(appState.totalCaloriesToday).toString()
                    label: "Consumed"; isDark: root.isDark
                    valueColor: "#06b6d4"
                }
                StatCard {
                    width: (parent.width - 12) / 2
                    value: Math.round(appState.dailyGoal).toString()
                    label: "Goal"; isDark: root.isDark
                    valueColor: "#22c55e"
                }
            }

            // Activity strip
            Rectangle {
                width: parent.width; radius: 12; color: surface; height: 110

                Column {
                    anchors { fill: parent; margins: 16 }
                    spacing: 10
                    Text { text: "ACTIVITY TODAY"; font.pixelSize: 16; font.letterSpacing: 1; color: muted }
                    Row {
                        width: parent.width
                        spacing: 0
                        StatCard {
                            width: (parent.width - 2) / 3; height: 60
                            value: (appState.healthSnapshot.steps || 0).toString()
                            label: "Steps"; isDark: root.isDark
                            valueColor: "#6366f1"
                        }
                        Rectangle { width: 1; height: 60; color: isDark ? "#334155" : "#e2e8f0" }
                        StatCard {
                            width: (parent.width - 2) / 3; height: 60
                            value: (appState.healthSnapshot.active_minutes || 0).toString()
                            label: "Active minutes"; isDark: root.isDark
                            valueColor: "#06b6d4"
                        }
                        Rectangle { width: 1; height: 60; color: isDark ? "#334155" : "#e2e8f0" }
                        StatCard {
                            width: (parent.width - 2) / 3; height: 60
                            value: Math.round(appState.healthSnapshot.calories_burned || 0).toString()
                            label: "Burned"; isDark: root.isDark
                            valueColor: "#f59e0b"
                        }
                    }
                }
            }

            // Last scans card (up to 3)
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: lastScanCol.implicitHeight + 24
                visible: appState.todaysLog.length > 0

                Column {
                    id: lastScanCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 8

                    Text {
                        text: "LAST SCANS"
                        font { pixelSize: 16; letterSpacing: 1 }
                        color: muted
                    }

                    Repeater {
                        model: Math.min(3, appState.todaysLog.length)
                        RowLayout {
                            width: lastScanCol.width
                            spacing: 12

                            ColumnLayout {
                                Layout.alignment: Qt.AlignVCenter
                                spacing: 2
                                Text {
                                    text: appState.todaysLog[index].food_name
                                    font { pixelSize: 26; bold: true }
                                    color: isDark ? "white" : "#0f172a"
                                }
                                Text {
                                    text: appState.todaysLog[index].weight_g + "g · " +
                                          Math.round(appState.todaysLog[index].calories) + " kcal"
                                    font.pixelSize: 18; color: muted
                                }
                            }
                            Item { Layout.fillWidth: true }
                            HealthBadge {
                                Layout.alignment: Qt.AlignVCenter
                                healthy: appState.todaysLog[index].is_healthy === 1
                                isDark: root.isDark
                            }
                        }
                    }
                }
            }
        }
    }
}
