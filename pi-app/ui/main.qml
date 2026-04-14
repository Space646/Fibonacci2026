import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15

ApplicationWindow {
    id: root
    visible: true
    width: 720
    height: 1280
    title: "Fibonacci Health"
    color: appState.theme === "dark" ? "#0f172a" : "#f8fafc"

    // Test mode banner
    Rectangle {
        id: testBanner
        visible: appState.testMode
        anchors { top: parent.top; left: parent.left; right: parent.right }
        height: 32
        color: "#92400e"
        z: 100
        Text {
            anchors.centerIn: parent
            text: "⚠  TEST MODE ACTIVE"
            color: "#fbbf24"
            font { pixelSize: 11; bold: true; letterSpacing: 1 }
        }
    }

    StackView {
        id: stackView
        anchors {
            top: appState.testMode ? testBanner.bottom : parent.top
            left: parent.left; right: parent.right; bottom: navBar.top
        }
        initialItem: Qt.resolvedUrl("screens/Dashboard.qml")
    }

    // Bottom navigation bar
    Rectangle {
        id: navBar
        anchors { bottom: parent.bottom; left: parent.left; right: parent.right }
        height: 64
        color: appState.theme === "dark" ? "#0f172a" : "#ffffff"

        Rectangle {
            anchors { top: parent.top; left: parent.left; right: parent.right }
            height: 1
            color: appState.theme === "dark" ? "#1e293b" : "#e2e8f0"
        }

        Row {
            anchors.fill: parent
            spacing: 0

            Repeater {
                model: [
                    { label: "Home",     screen: "screens/Dashboard.qml" },
                    { label: "Scan",     screen: "screens/ScanResult.qml" },
                    { label: "Log",      screen: "screens/FoodLog.qml" },
                    { label: "Settings", screen: "screens/Settings.qml" }
                ]

                Rectangle {
                    width: navBar.width / 4
                    height: navBar.height
                    color: "transparent"

                    Column {
                        anchors.centerIn: parent
                        spacing: 4

                        Rectangle {
                            width: 24; height: 3; radius: 2
                            anchors.horizontalCenter: parent.horizontalCenter
                            color: stackView.currentItem &&
                                   stackView.currentItem.objectName === modelData.label
                                   ? "#6366f1" : "transparent"
                        }

                        Text {
                            anchors.horizontalCenter: parent.horizontalCenter
                            text: modelData.label
                            font.pixelSize: 11
                            color: stackView.currentItem &&
                                   stackView.currentItem.objectName === modelData.label
                                   ? "#6366f1"
                                   : (appState.theme === "dark" ? "#475569" : "#94a3b8")
                        }
                    }

                    MouseArea {
                        anchors.fill: parent
                        onClicked: stackView.replace(Qt.resolvedUrl(modelData.screen))
                    }
                }
            }
        }
    }

    Connections {
        target: appState
        function onScanDetected(food) {
            stackView.replace(Qt.resolvedUrl("screens/ScanResult.qml"),
                              { detectedFood: food })
        }
    }
}
