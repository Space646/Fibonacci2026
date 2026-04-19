import QtQuick 2.15

Rectangle {
    id: root
    property string value: "0"
    property string label: ""
    property color valueColor: "#06b6d4"
    property bool isDark: true

    height: 90; radius: 10
    color: isDark ? "#1e293b" : "#ffffff"

    Column {
        anchors.centerIn: parent
        spacing: 4

        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            text: root.value
            font { pixelSize: 28; bold: true }
            color: root.valueColor
        }
        Text {
            anchors.horizontalCenter: parent.horizontalCenter
            text: root.label
            font.pixelSize: 18
            color: root.isDark ? "#64748b" : "#94a3b8"
        }
    }
}
