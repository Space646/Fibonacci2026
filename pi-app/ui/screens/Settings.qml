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

                        RowLayout {
                            anchors { fill: parent; leftMargin: 14; rightMargin: 14 }
                            spacing: 12
                            Rectangle {
                                width: 36; height: 36; radius: 18
                                Layout.alignment: Qt.AlignVCenter
                                gradient: Gradient {
                                    orientation: Gradient.Horizontal
                                    GradientStop { position: 0.0; color: "#6366f1" }
                                    GradientStop { position: 1.0; color: "#06b6d4" }
                                }
                                Text { anchors.centerIn: parent
                                       text: appState.activeUserName[0].toUpperCase()
                                       font.pixelSize: 18; font.bold: true; color: "white" }
                            }
                            Text { text: appState.activeUserName
                                   font.pixelSize: 22; color: isDark ? "white" : "#0f172a"
                                   Layout.alignment: Qt.AlignVCenter }
                            Item { Layout.fillWidth: true }
                            Row {
                                spacing: 4; Layout.alignment: Qt.AlignVCenter
                                Rectangle { width: 6; height: 6; radius: 3; color: "#22c55e"
                                            anchors.verticalCenter: parent.verticalCenter }
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
