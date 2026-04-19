import QtQuick 2.15

Rectangle {
    id: root
    property bool healthy: true
    property bool isDark: true
    property string customText: ""

    height: 36; radius: 18
    width: badgeText.implicitWidth + 28
    color: healthy
           ? (isDark ? "#064e3b" : "#d1fae5")
           : (isDark ? "#450a0a" : "#fee2e2")

    Text {
        id: badgeText
        anchors.centerIn: parent
        text: root.customText !== ""
              ? root.customText
              : (root.healthy ? "✓ Healthy" : "✗ Unhealthy")
        font { pixelSize: 20; bold: true }
        color: root.healthy
               ? (root.isDark ? "#34d399" : "#065f46")
               : (root.isDark ? "#f87171" : "#991b1b")
    }
}
