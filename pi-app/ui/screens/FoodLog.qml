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
        anchors { fill: parent; margins: 24 }
        spacing: 20

        // Header
        RowLayout {
            width: parent.width
            spacing: 12

            Text {
                text: "Today's Log"
                font { pixelSize: 36; bold: true }
                color: isDark ? "white" : "#0f172a"
            }
            Item { Layout.fillWidth: true }
            Text {
                text: Math.round(appState.totalCaloriesToday) + " kcal total"
                font.pixelSize: 22; color: "#06b6d4"
                Layout.alignment: Qt.AlignVCenter | Qt.AlignRight
            }
        }

        // Empty state
        Item {
            width: parent.width; height: 180
            visible: appState.todaysLog.length === 0
            Text {
                anchors.centerIn: parent
                text: "No foods logged yet.\nScan something!"
                horizontalAlignment: Text.AlignHCenter
                font.pixelSize: 26; color: muted
            }
        }

        // Log list
        ListView {
            width: parent.width
            height: parent.height - 80
            model: appState.todaysLog
            spacing: 12
            clip: true

            delegate: Rectangle {
                width: ListView.view ? ListView.view.width : 0
                height: 90; radius: 12; color: surface

                RowLayout {
                    anchors { fill: parent; leftMargin: 12; rightMargin: 12 }
                    spacing: 12

                    ColumnLayout {
                        Layout.alignment: Qt.AlignVCenter
                        spacing: 4
                        Text { text: modelData.food_name
                               font { pixelSize: 26; bold: true }
                               color: isDark ? "white" : "#0f172a" }
                        Text { text: modelData.weight_g + "g"
                               font.pixelSize: 18; color: muted }
                    }

                    Item { Layout.fillWidth: true }

                    Text {
                        text: Math.round(modelData.calories) + " kcal"
                        font.pixelSize: 24; font.bold: true; color: "#06b6d4"
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
                        width: 48; height: 48; radius: 8
                        color: "transparent"
                        Text { anchors.centerIn: parent; text: "✕"
                               font.pixelSize: 26; color: "#f87171" }
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
