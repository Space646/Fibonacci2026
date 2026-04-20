import QtQuick 2.15

Item {
    id: root
    height: 32
    property string label: "Fiber"
    property real value: 0
    property real maxValue: 10
    property color barColor: "#34d399"
    property bool isDark: true

    Row {
        anchors { fill: parent; rightMargin: 12 }
        spacing: 8

        Text {
            width: 72; height: parent.height
            text: root.label
            font.pixelSize: 20
            color: root.barColor
            verticalAlignment: Text.AlignVCenter
        }

        Rectangle {
            height: 8; width: parent.width - 72 - 8 - 48
            anchors.verticalCenter: parent.verticalCenter
            radius: 3
            color: root.isDark ? "#0f172a" : "#e2e8f0"

            Rectangle {
                height: parent.height
                width: Math.min(root.value / root.maxValue, 1.0) * parent.width
                radius: 3
                color: root.barColor
            }
        }

        Text {
            width: 48; height: parent.height
            text: root.value + "g"
            font.pixelSize: 20
            color: root.barColor
            horizontalAlignment: Text.AlignRight
            verticalAlignment: Text.AlignVCenter
        }
    }
}
