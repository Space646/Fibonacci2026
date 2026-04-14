import SwiftUI

struct CalorieRingView: View {
    let consumed: Double
    let goal: Double
    @EnvironmentObject var env: AppEnvironment

    private var progress: Double { goal > 0 ? min(consumed / goal, 1.0) : 0 }
    private var remaining: Double { max(goal - consumed, 0) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(env.theme.bgSurface, lineWidth: 14)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [Color(hex: "6366f1"), Color(hex: "06b6d4")]),
                        center: .center
                    ),
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.5), value: progress)
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
