import QtQuick 2.15
import QtQuick.Layouts 1.15
import "../components"

Item {
    objectName: "Home"
    anchors.fill: parent

    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Flickable {
        anchors { fill: parent; margins: 16 }
        contentHeight: col.implicitHeight
        clip: true

        Column {
            id: col
            width: parent.width
            spacing: 12

            // Header
            Row {
                width: parent.width
                spacing: 10

                Rectangle {
                    width: 40; height: 40; radius: 20
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: "#6366f1" }
                        GradientStop { position: 1.0; color: "#06b6d4" }
                    }
                    Text {
                        anchors.centerIn: parent
                        text: appState.activeUserName.length > 0
                              ? appState.activeUserName[0].toUpperCase() : "G"
                        color: "white"; font { pixelSize: 16; bold: true }
                    }
                }

                Column {
                    anchors.verticalCenter: parent.verticalCenter
                    spacing: 2
                    Text {
                        text: appState.activeUserName
                        color: isDark ? "white" : "#0f172a"
                        font { pixelSize: 14; bold: true }
                    }
                    Text {
                        text: Qt.formatDate(new Date(), "dddd d MMM")
                        color: muted; font.pixelSize: 10
                    }
                }

                Item { width: 1; Layout.fillWidth: true }

                // Connect prompt when no user
                Rectangle {
                    visible: !appState.userConnected
                    color: surface; radius: 6; height: 28
                    width: connectText.implicitWidth + 16
                    anchors.verticalCenter: parent.verticalCenter
                    Text {
                        id: connectText
                        anchors.centerIn: parent
                        text: "Connect phone"
                        color: "#6366f1"; font.pixelSize: 10
                    }
                }
            }

            // Calorie ring
            CalorieRing {
                anchors.horizontalCenter: parent.horizontalCenter
                width: 180; height: 180
                consumed: appState.totalCaloriesToday
                goal: appState.dailyGoal
                isDark: parent.parent.parent.isDark
            }

            // Stats row
            Row {
                width: parent.width
                spacing: 8
                StatCard {
                    width: (parent.width - 16) / 3
                    value: Math.round(appState.totalCaloriesToday).toString()
                    label: "consumed"; isDark: col.parent.parent.isDark
                    valueColor: "#06b6d4"
                }
                StatCard {
                    width: (parent.width - 16) / 3
                    value: Math.round(appState.dailyGoal).toString()
                    label: "goal"; isDark: col.parent.parent.isDark
                    valueColor: "#22c55e"
                }
                StatCard {
                    width: (parent.width - 16) / 3
                    value: Math.round(appState.healthSnapshot.calories_burned || 0).toString()
                    label: "burned"; isDark: col.parent.parent.isDark
                    valueColor: "#f59e0b"
                }
            }

            // Last scan card
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: lastScanCol.implicitHeight + 24
                visible: appState.todaysLog.length > 0

                Column {
                    id: lastScanCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 8

                    Text {
                        text: "LAST SCAN"
                        font { pixelSize: 9; letterSpacing: 1 }
                        color: muted
                    }
                    Row {
                        width: parent.width
                        Column {
                            spacing: 2
                            Text {
                                text: appState.todaysLog[0] ? appState.todaysLog[0].food_name : ""
                                font { pixelSize: 15; bold: true }
                                color: isDark ? "white" : "#0f172a"
                            }
                            Text {
                                text: appState.todaysLog[0]
                                      ? (appState.todaysLog[0].weight_g + "g · " +
                                         Math.round(appState.todaysLog[0].calories) + " kcal")
                                      : ""
                                font.pixelSize: 10; color: muted
                            }
                        }
                        Item { Layout.fillWidth: true; width: 1 }
                        HealthBadge {
                            anchors.verticalCenter: parent.verticalCenter
                            healthy: appState.todaysLog[0] ? appState.todaysLog[0].is_healthy === 1 : true
                            isDark: col.parent.parent.isDark
                        }
                    }
                }
            }

            // Activity strip
            Rectangle {
                width: parent.width; radius: 10; color: surface; height: 72

                Column {
                    anchors { fill: parent; margins: 12 }
                    spacing: 6
                    Text { text: "ACTIVITY TODAY"; font { pixelSize: 9; letterSpacing: 1 }; color: muted }
                    Row {
                        width: parent.width
                        spacing: 0
                        StatCard {
                            width: (parent.width - 2) / 3; height: 40
                            value: (appState.healthSnapshot.steps || 0).toString()
                            label: "steps"; isDark: col.parent.parent.isDark
                            valueColor: "#6366f1"
                        }
                        Rectangle { width: 1; height: 40; color: isDark ? "#334155" : "#e2e8f0" }
                        StatCard {
                            width: (parent.width - 2) / 3; height: 40
                            value: (appState.healthSnapshot.active_minutes || 0).toString()
                            label: "active min"; isDark: col.parent.parent.isDark
                            valueColor: "#06b6d4"
                        }
                        Rectangle { width: 1; height: 40; color: isDark ? "#334155" : "#e2e8f0" }
                        StatCard {
                            width: (parent.width - 2) / 3; height: 40
                            value: Math.round(appState.healthSnapshot.calories_burned || 0).toString()
                            label: "kcal burned"; isDark: col.parent.parent.isDark
                            valueColor: "#f59e0b"
                        }
                    }
                }
            }
        }
    }
}
