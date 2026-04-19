import QtQuick 2.15
import QtQuick.Layouts 1.15
import "../components"

Item {
    id: root
    objectName: "Scan"

    property var detectedFood: ({})
    property real weightG: appState.currentWeightG
    property real calories: detectedFood.calories_per_100g !== undefined
                            ? appState.calculateCalories(detectedFood.calories_per_100g, weightG)
                            : 0

    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"

    Flickable {
        anchors { fill: parent; margins: 24 }
        contentHeight: col.implicitHeight; clip: true

        Column {
            id: col
            width: parent.width; spacing: 20

            // Food name + health badge
            Column {
                width: parent.width; spacing: 6
                anchors.horizontalCenter: parent.horizontalCenter

                HealthBadge {
                    anchors.horizontalCenter: parent.horizontalCenter
                    healthy: detectedFood.is_healthy === 1
                    isDark: root.isDark
                }
                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: detectedFood.name || "Unknown Food"
                    font { pixelSize: 46; bold: true }
                    color: isDark ? "white" : "#0f172a"
                }
                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: "Label ID " + (detectedFood.huskylens_label_id || "—")
                    font.pixelSize: 18; color: muted
                }
            }

            // Weight + calories cards
            Row {
                width: parent.width; spacing: 12
                Rectangle {
                    width: (parent.width - 12) / 2; height: 120; radius: 12; color: surface
                    Column {
                        anchors.centerIn: parent; spacing: 4
                        Text { anchors.horizontalCenter: parent.horizontalCenter; text: "WEIGHT"
                               font.pixelSize: 16; font.letterSpacing: 1; color: muted }
                        Text { anchors.horizontalCenter: parent.horizontalCenter
                               text: Math.round(weightG).toString()
                               font.pixelSize: 46; font.bold: true; color: isDark ? "white" : "#0f172a" }
                        Text { anchors.horizontalCenter: parent.horizontalCenter; text: "grams"
                               font.pixelSize: 18; color: muted }
                    }
                }
                Rectangle {
                    width: (parent.width - 12) / 2; height: 120; radius: 12
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: "#312e81" }
                        GradientStop { position: 1.0; color: "#164e63" }
                    }
                    Column {
                        anchors.centerIn: parent; spacing: 4
                        Text { anchors.horizontalCenter: parent.horizontalCenter; text: "CALORIES"
                               font.pixelSize: 16; font.letterSpacing: 1; color: "#a5b4fc" }
                        Text { anchors.horizontalCenter: parent.horizontalCenter
                               text: Math.round(calories).toString()
                               font.pixelSize: 46; font.bold: true; color: "white" }
                        Text { anchors.horizontalCenter: parent.horizontalCenter; text: "kcal"
                               font.pixelSize: 18; color: "#a5b4fc" }
                    }
                }
            }

            // Macro bars
            Rectangle {
                width: parent.width; radius: 10; color: surface
                height: macroCol.implicitHeight + 24

                Column {
                    id: macroCol
                    anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
                    spacing: 8

                    Text { text: "NUTRITIONAL BREAKDOWN / 100g"
                           font.pixelSize: 16; font.letterSpacing: 1; color: muted }

                    MacroBar { width: parent.width; label: "Fiber"
                               value: detectedFood.fiber_per_100g || 0; maxValue: 10
                               barColor: "#34d399"; isDark: root.isDark }
                    MacroBar { width: parent.width; label: "Protein"
                               value: detectedFood.protein_per_100g || 0; maxValue: 30
                               barColor: "#60a5fa"; isDark: root.isDark }
                    MacroBar { width: parent.width; label: "Sugar"
                               value: detectedFood.sugar_per_100g || 0; maxValue: 30
                               barColor: "#f87171"; isDark: root.isDark }
                    MacroBar { width: parent.width; label: "Fat"
                               value: detectedFood.fat_per_100g || 0; maxValue: 30
                               barColor: "#fbbf24"; isDark: root.isDark }

                    // Health score bar
                    Row {
                        width: parent.width
                        Text { width: 120; text: "Health Score"; font.pixelSize: 18; color: muted
                               verticalAlignment: Text.AlignVCenter; height: 24 }
                        Rectangle {
                            height: 8; width: parent.width - 120 - 60; radius: 4
                            anchors.verticalCenter: parent.verticalCenter
                            color: isDark ? "#0f172a" : "#e2e8f0"
                            Rectangle {
                                height: parent.height; radius: 3
                                width: ((detectedFood.health_score || 50) / 100) * parent.width
                                gradient: Gradient {
                                    orientation: Gradient.Horizontal
                                    GradientStop { position: 0.0; color: "#6366f1" }
                                    GradientStop { position: 1.0; color: "#06b6d4" }
                                }
                            }
                        }
                        Text { width: 60; text: (detectedFood.health_score || 50) + "/100"
                               font.pixelSize: 18; font.bold: true; color: "#06b6d4"
                               horizontalAlignment: Text.AlignRight
                               verticalAlignment: Text.AlignVCenter; height: 24 }
                    }
                }
            }

            // Remaining after this
            Rectangle {
                width: parent.width; height: 64; radius: 10; color: surface
                Row {
                    anchors { fill: parent; leftMargin: 12; rightMargin: 12 }
                    Text { anchors.verticalCenter: parent.verticalCenter; text: "Remaining after this"
                           font.pixelSize: 20; color: muted }
                    Item { Layout.fillWidth: true; width: 1 }
                    Text { anchors.verticalCenter: parent.verticalCenter
                           text: Math.round(Math.max(0, appState.remainingCalories - calories)) + " kcal"
                           font.pixelSize: 27; font.bold: true; color: "#06b6d4" }
                }
            }

            // Action buttons
            Row {
                width: parent.width; spacing: 8
                Rectangle {
                    width: (parent.width - 8) / 3; height: 64; radius: 10; color: surface
                    border { color: isDark ? "#334155" : "#e2e8f0"; width: 1 }
                    Text { anchors.centerIn: parent; text: "Discard"
                           font.pixelSize: 22; font.bold: true; color: muted }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: stackView.replace(Qt.resolvedUrl("Dashboard.qml"))
                    }
                }
                Rectangle {
                    width: (parent.width - 8) * 2 / 3; height: 64; radius: 10
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: "#6366f1" }
                        GradientStop { position: 1.0; color: "#06b6d4" }
                    }
                    Text { anchors.centerIn: parent; text: "Add to Log"
                           font.pixelSize: 22; font.bold: true; color: "white" }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: {
                            if (detectedFood.id !== undefined) {
                                appState.confirmScan(detectedFood.id, weightG, calories)
                            }
                            stackView.replace(Qt.resolvedUrl("Dashboard.qml"))
                        }
                    }
                }
            }
        }
    }
}
