import QtQuick 2.15
import QtQuick.Shapes 1.15

Item {
    id: root
    width: 240; height: 240

    property real consumed: 0
    property real goal: 2000
    property real rawRatio: goal > 0 ? consumed / goal : 0
    property real progress: Math.min(rawRatio, 1.0)
    property bool isDark: true

    // Color shifts as the user eats more of their daily goal:
    //   <60%  green      (plenty of room)
    //   60-85% indigo    (on track)
    //   85-100% amber    (approaching goal)
    //   >=100% red       (at / over goal)
    property color progressColor:
        rawRatio >= 1.0  ? "#ef4444" :
        rawRatio >= 0.85 ? "#f59e0b" :
        rawRatio >= 0.60 ? "#6366f1" :
                           "#22c55e"

    // Background track
    Shape {
        anchors.fill: parent
        ShapePath {
            strokeColor: isDark ? "#1e293b" : "#e2e8f0"
            strokeWidth: 18
            fillColor: "transparent"
            capStyle: ShapePath.RoundCap
            PathAngleArc {
                centerX: root.width / 2; centerY: root.height / 2
                radiusX: root.width / 2 - 14; radiusY: root.height / 2 - 14
                startAngle: -90; sweepAngle: 360
            }
        }
    }

    // Progress arc — color shifts based on how close we are to the goal.
    Shape {
        anchors.fill: parent
        visible: root.progress > 0
        ShapePath {
            strokeColor: root.progressColor
            strokeWidth: 18
            fillColor: "transparent"
            capStyle: ShapePath.RoundCap

            Behavior on strokeColor {
                ColorAnimation { duration: 260; easing.type: Easing.InOutQuad }
            }

            PathAngleArc {
                centerX: root.width / 2; centerY: root.height / 2
                radiusX: root.width / 2 - 14; radiusY: root.height / 2 - 14
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
            text: Math.max(0, Math.round(goal - consumed))
            font { pixelSize: 50; bold: true }
            color: isDark ? "#ffffff" : "#0f172a"
        }
        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            text: "kcal left"
            font.pixelSize: 20
            color: isDark ? "#64748b" : "#94a3b8"
        }
    }
}
