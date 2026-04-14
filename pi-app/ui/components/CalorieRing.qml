import QtQuick 2.15
import QtQuick.Shapes 1.15

Item {
    id: root
    width: 180; height: 180

    property real consumed: 0
    property real goal: 2000
    property real progress: goal > 0 ? Math.min(consumed / goal, 1.0) : 0
    property bool isDark: true

    // Background track
    Shape {
        anchors.fill: parent
        ShapePath {
            strokeColor: isDark ? "#1e293b" : "#e2e8f0"
            strokeWidth: 14
            fillColor: "transparent"
            capStyle: ShapePath.RoundCap
            PathAngleArc {
                centerX: root.width / 2; centerY: root.height / 2
                radiusX: root.width / 2 - 10; radiusY: root.height / 2 - 10
                startAngle: -90; sweepAngle: 360
            }
        }
    }

    // Progress arc (indigo → cyan via gradient workaround using two arcs)
    Shape {
        anchors.fill: parent
        visible: root.progress > 0
        ShapePath {
            strokeColor: "#6366f1"
            strokeWidth: 14
            fillColor: "transparent"
            capStyle: ShapePath.RoundCap
            PathAngleArc {
                centerX: root.width / 2; centerY: root.height / 2
                radiusX: root.width / 2 - 10; radiusY: root.height / 2 - 10
                startAngle: -90
                sweepAngle: 360 * root.progress
            }
        }
    }

    // Center text
    Column {
        anchors.centerIn: parent
        spacing: 2

        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            text: Math.round(goal - consumed)
            font { pixelSize: 28; bold: true }
            color: isDark ? "#ffffff" : "#0f172a"
        }
        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            text: "kcal left"
            font.pixelSize: 11
            color: isDark ? "#64748b" : "#94a3b8"
        }
    }
}
