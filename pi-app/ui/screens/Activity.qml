import QtQuick 2.15
import "../components"

Item {
    objectName: "Activity"
    anchors.fill: parent
    property bool isDark: appState.theme === "dark"
    property color surface: isDark ? "#1e293b" : "#ffffff"
    property color muted:   isDark ? "#64748b" : "#94a3b8"
    property var snap: appState.healthSnapshot

    Column {
        anchors { fill: parent; margins: 24 }
        spacing: 20

        Text { text: "Activity"; font { pixelSize: 36; bold: true }
               color: isDark ? "white" : "#0f172a" }

        Text { text: !appState.userConnected
                     ? "Connect your phone to sync activity data."
                     : "Synced from HealthKit via Bluetooth."
               font.pixelSize: 22; color: muted }

        // Big stats grid
        Grid {
            width: parent.width; columns: 2; spacing: 10

            StatCard { width: (parent.width - 10) / 2; height: 120
                       value: (snap.steps || 0).toString(); label: "Steps"
                       valueColor: "#6366f1"; isDark: appState.theme === "dark" }
            StatCard { width: (parent.width - 10) / 2; height: 120
                       value: (snap.active_minutes || 0).toString(); label: "Active Minutes"
                       valueColor: "#06b6d4"; isDark: appState.theme === "dark" }
            StatCard { width: (parent.width - 10) / 2; height: 120
                       value: Math.round(snap.calories_burned || 0).toString(); label: "Calories Burned"
                       valueColor: "#f59e0b"; isDark: appState.theme === "dark" }
            StatCard { width: (parent.width - 10) / 2; height: 120
                       value: (snap.workouts || 0).toString(); label: "Workouts"
                       valueColor: "#22c55e"; isDark: appState.theme === "dark" }
        }
    }
}
