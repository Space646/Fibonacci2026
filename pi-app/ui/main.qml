import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

ApplicationWindow {
    id: root
    visible: true
    visibility: Window.FullScreen
    width: 720
    height: 1280
    title: "Fibonacci Health"
    color: appState.theme === "dark" ? "#0f172a" : "#f8fafc"

    function openAdminMenu() { adminOverlay.open() }

    StackView {
        id: stackView
        anchors {
            top: parent.top
            left: parent.left; right: parent.right; bottom: navBar.top
        }
        initialItem: Qt.createComponent(Qt.resolvedUrl("screens/Dashboard.qml"))

        // Direction of the next replace() — set by nav bar before switching.
        //  1  = target is to the right of current (slide new page in from right)
        // -1  = target is to the left (slide new page in from left)
        property int slideDir: 1
        property int currentTabIndex: 0

        replaceEnter: Transition {
            XAnimator {
                from: stackView.slideDir * stackView.width
                to: 0
                duration: 220
                easing.type: Easing.OutCubic
            }
        }
        replaceExit: Transition {
            XAnimator {
                from: 0
                to: -stackView.slideDir * stackView.width
                duration: 220
                easing.type: Easing.OutCubic
            }
        }
    }

    // Bottom navigation bar
    Rectangle {
        id: navBar
        anchors { bottom: parent.bottom; left: parent.left; right: parent.right }
        height: 90
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
                            width: 36; height: 4; radius: 2
                            anchors.horizontalCenter: parent.horizontalCenter
                            color: stackView.currentItem &&
                                   stackView.currentItem.objectName === modelData.label
                                   ? "#6366f1"
                                   : (appState.theme === "dark" ? "#475569" : "#94a3b8")
                        }

                        Text {
                            anchors.horizontalCenter: parent.horizontalCenter
                            text: modelData.label
                            font.pixelSize: 20
                            color: stackView.currentItem &&
                                   stackView.currentItem.objectName === modelData.label
                                   ? "#6366f1"
                                   : (appState.theme === "dark" ? "#475569" : "#94a3b8")
                        }
                    }

                    MouseArea {
                        anchors.fill: parent
                        onClicked: {
                            var alreadyHere = stackView.currentItem &&
                                              stackView.currentItem.objectName === modelData.label
                            if (alreadyHere) return
                            stackView.slideDir = index >= stackView.currentTabIndex ? 1 : -1
                            stackView.currentTabIndex = index
                            stackView.replace(Qt.resolvedUrl(modelData.screen))
                        }
                    }
                }
            }
        }
    }

    AdminOverlay {
        id: adminOverlay
        anchors.fill: parent
    }

    Connections {
        target: appState
        function onScanDetected(food) {
            const scanIndex = 1
            stackView.slideDir = scanIndex >= stackView.currentTabIndex ? 1 : -1
            stackView.currentTabIndex = scanIndex
            stackView.replace(Qt.resolvedUrl("screens/ScanResult.qml"),
                              { detectedFood: food })
        }
    }
}
