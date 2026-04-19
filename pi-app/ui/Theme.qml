pragma Singleton
import QtQuick 2.15

QtObject {
    id: root

    property bool isDark: true

    // Backgrounds
    property color bgPrimary:   isDark ? "#0f172a" : "#f8fafc"
    property color bgSurface:   isDark ? "#1e293b" : "#ffffff"
    property color bgBorder:    isDark ? "#334155" : "#e2e8f0"

    // Text
    property color textPrimary: isDark ? "#ffffff"  : "#0f172a"
    property color textMuted:   isDark ? "#64748b"  : "#94a3b8"

    // Accent gradient endpoints (used in CalorieRing, primary buttons)
    property color accentStart: "#6366f1"  // indigo
    property color accentEnd:   "#06b6d4"  // cyan

    // Semantic
    property color healthy:     isDark ? "#34d399" : "#065f46"
    property color healthyBg:   isDark ? "#064e3b" : "#d1fae5"
    property color unhealthy:   isDark ? "#f87171" : "#991b1b"
    property color unhealthyBg: isDark ? "#450a0a" : "#fee2e2"
    property color caloriesBurned: "#f59e0b"

    // Typography
    property string fontFamily: "Inter, system-ui, sans-serif"
    property int fontSizeSmall:  20
    property int fontSizeBody:   26
    property int fontSizeTitle:  36
    property int fontSizeHero:   50

    // Spacing
    property int radiusCard:   14
    property int radiusButton:  12
    property int paddingPage:  24
}
