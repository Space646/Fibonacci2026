import QtQuick 2.15
import QtQuick.Layouts 1.15
import "../components"

Item {
    id: root
    objectName: "Log"
    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Column {
        anchors { fill: parent; margins: 16 }
        spacing: 12

        // Header
        RowLayout {
            width: parent.width
            spacing: 12

            Text {
                text: "Today's Log"
                font { pixelSize: 20; bold: true }
                color: isDark ? "white" : "#0f172a"
            }
            Item { Layout.fillWidth: true }
            Text {
                text: Math.round(appState.totalCaloriesToday) + " kcal total"
                font.pixelSize: 12; color: "#06b6d4"
                Layout.alignment: Qt.AlignVCenter | Qt.AlignRight
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
                width: ListView.view ? ListView.view.width : 0
                height: 60; radius: 10; color: surface

                RowLayout {
                    anchors { fill: parent; leftMargin: 12; rightMargin: 12 }
                    spacing: 12

                    ColumnLayout {
                        Layout.alignment: Qt.AlignVCenter
                        spacing: 4
                        Text { text: modelData.food_name
                               font { pixelSize: 14; bold: true }
                               color: isDark ? "white" : "#0f172a" }
                        Text { text: modelData.weight_g + "g"
                               font.pixelSize: 10; color: muted }
                    }

                    Item { Layout.fillWidth: true }

                    Text {
                        text: Math.round(modelData.calories) + " kcal"
                        font.pixelSize: 13; font.bold: true; color: "#06b6d4"
                        Layout.alignment: Qt.AlignVCenter
                    }

                    HealthBadge {
                        Layout.alignment: Qt.AlignVCenter
                        Layout.leftMargin: 4
                        healthy: modelData.is_healthy === 1
                        isDark: root.isDark
                    }

                    // Delete button — always pinned to the right
                    Rectangle {
                        Layout.alignment: Qt.AlignVCenter
                        Layout.leftMargin: 8
                        width: 32; height: 32; radius: 6
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
