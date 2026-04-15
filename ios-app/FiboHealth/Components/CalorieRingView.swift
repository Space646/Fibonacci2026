import SwiftUI

struct CalorieRingView: View {
    let consumed: Double
    let goal: Double
    @EnvironmentObject var env: AppEnvironment

    private var rawRatio: Double { goal > 0 ? consumed / goal : 0 }
    private var progress: Double { min(rawRatio, 1.0) }
    private var remaining: Double { max(goal - consumed, 0) }

    // Match Pi CalorieRing.qml: color shifts with progress toward daily goal.
    //   <60%  green   (plenty of room)
    //   60-85% indigo (on track)
    //   85-100% amber (approaching goal)
    //   >=100% red    (at / over goal)
    private var progressColor: Color {
        if rawRatio >= 1.0  { return Color(hex: "ef4444") }
        if rawRatio >= 0.85 { return Color(hex: "f59e0b") }
        if rawRatio >= 0.60 { return Color(hex: "6366f1") }
        return Color(hex: "22c55e")
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(env.theme.bgSurface, lineWidth: 14)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    progressColor,
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.5), value: progress)
                .animation(.easeInOut(duration: 0.26), value: progressColor)
            VStack(spacing: 2) {
                Text("\(Int(remaining))")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(env.theme.textPrimary)
                Text("kcal left")
                    .font(.system(size: 11))
                    .foregroundColor(env.theme.textMuted)
            }
        }
    }
}
