import QtQuick 2.15

Item {
    id: root
    height: 20
    property string label: "Fiber"
    property real value: 0
    property real maxValue: 10
    property color barColor: "#34d399"
    property bool isDark: true

    Row {
        anchors.fill: parent
        spacing: 8

        Text {
            width: 48; height: parent.height
            text: root.label
            font.pixelSize: 11
            color: root.barColor
            verticalAlignment: Text.AlignVCenter
        }

        Rectangle {
            height: 5; width: parent.width - 48 - 8 - 32
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
            width: 32; height: parent.height
            text: root.value + "g"
            font.pixelSize: 11
            color: root.barColor
            horizontalAlignment: Text.AlignRight
            verticalAlignment: Text.AlignVCenter
        }
    }
}
