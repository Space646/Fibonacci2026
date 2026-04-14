import QtQuick 2.15
import "../components"

Item {
    objectName: "Log"
    anchors.fill: parent
    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Column {
        anchors { fill: parent; margins: 16 }
        spacing: 12

        // Header
        Row {
            width: parent.width
            Text { text: "Today's Log"; font { pixelSize: 20; bold: true }
                   color: isDark ? "white" : "#0f172a" }
            Item { width: 1; Layout.fillWidth: true }
            Text {
                anchors.verticalCenter: parent.verticalCenter
                text: Math.round(appState.totalCaloriesToday) + " kcal total"
                font.pixelSize: 12; color: "#06b6d4"
            }
        }

        // Empty state
        Item {
            width: parent.width; height: 120
            visible: appState.todaysLog.length === 0
            Text {
                anchors.centerIn: parent
                text: "No foods logged yet.\nScan something!"
                horizontalAlignment: Text.AlignHCenter
                font.pixelSize: 14; color: muted
            }
        }

        // Log list
        ListView {
            width: parent.width
            height: parent.height - 60
            model: appState.todaysLog
            spacing: 8
            clip: true

            delegate: Rectangle {
                width: parent.width; height: 60; radius: 10; color: surface

                Row {
                    anchors { fill: parent; leftMargin: 12; rightMargin: 12 }
                    spacing: 0

                    Column {
                        anchors.verticalCenter: parent.verticalCenter; spacing: 4
                        Text { text: modelData.food_name
                               font { pixelSize: 14; bold: true }
                               color: isDark ? "white" : "#0f172a" }
                        Text { text: modelData.weight_g + "g"
                               font.pixelSize: 10; color: muted }
                    }

                    Item { Layout.fillWidth: true; width: 1 }

                    Column {
                        anchors.verticalCenter: parent.verticalCenter
                        spacing: 4; horizontalItemAlignment: Column.AlignRight

                        Text { text: Math.round(modelData.calories) + " kcal"
                               font { pixelSize: 13; bold: true }; color: "#06b6d4"
                               anchors.right: parent.right }

                        HealthBadge {
                            healthy: modelData.is_healthy === 1
                            isDark: appState.theme === "dark"
                        }
                    }

                    // Delete button
                    Rectangle {
                        width: 32; height: 32; radius: 6
                        anchors.verticalCenter: parent.verticalCenter
                        color: "transparent"
                        Text { anchors.centerIn: parent; text: "✕"
                               font.pixelSize: 14; color: "#f87171" }
                        MouseArea {
                            anchors.fill: parent
                            onClicked: appState.deleteLogEntry(modelData.id)
                        }
                    }
                }
            }
        }
    }
}
