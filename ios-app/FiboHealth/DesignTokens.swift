import SwiftUI

enum Theme {
    case dark, light

    var bgPrimary: Color {
        self == .dark ? Color(hex: "0f172a") : Color(hex: "f8fafc")
    }
    var bgSurface: Color {
        self == .dark ? Color(hex: "1e293b") : Color.white
    }
    var bgBorder: Color {
        self == .dark ? Color(hex: "334155") : Color(hex: "e2e8f0")
    }
    var textPrimary: Color {
        self == .dark ? .white : Color(hex: "0f172a")
    }
    var textMuted: Color {
        self == .dark ? Color(hex: "64748b") : Color(hex: "94a3b8")
    }
    var healthy: Color       { Color(hex: "34d399") }
    var healthyBg: Color {
        self == .dark ? Color(hex: "064e3b") : Color(hex: "d1fae5")
    }
    var unhealthy: Color     { Color(hex: "f87171") }
    var unhealthyBg: Color {
        self == .dark ? Color(hex: "450a0a") : Color(hex: "fee2e2")
    }
    var caloriesBurned: Color { Color(hex: "f59e0b") }

    var accentGradient: LinearGradient {
        LinearGradient(
            colors: [Color(hex: "6366f1"), Color(hex: "06b6d4")],
            startPoint: .leading, endPoint: .trailing
        )
    }
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }
}
