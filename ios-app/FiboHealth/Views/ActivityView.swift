import SwiftUI

struct ActivityView: View {
    @EnvironmentObject var env: AppEnvironment
    @EnvironmentObject var healthKit: HealthKitService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    if !healthKit.isAuthorized {
                        VStack(spacing: 12) {
                            Image(systemName: "heart.slash")
                                .font(.system(size: 40))
                                .foregroundColor(env.theme.textMuted)
                            Text("HealthKit access not granted.\nOpen Settings → Health → FiboHealth to enable.")
                                .multilineTextAlignment(.center)
                                .font(.system(size: 13))
                                .foregroundColor(env.theme.textMuted)
                            Button("Request Access") { healthKit.requestAuthorization() }
                                .foregroundColor(Color(hex: "6366f1"))
                        }
                        .padding(.top, 40)
                    } else {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            StatCardView(value: "\(healthKit.snapshot.steps)",
                                         label: "Steps", valueColor: Color(hex: "6366f1"))
                                .frame(height: 80)
                            StatCardView(value: "\(healthKit.snapshot.activeMinutes)",
                                         label: "Active Minutes", valueColor: Color(hex: "06b6d4"))
                                .frame(height: 80)
                            StatCardView(value: "\(Int(healthKit.snapshot.caloriesBurned))",
                                         label: "Calories Burned", valueColor: env.theme.caloriesBurned)
                                .frame(height: 80)
                            StatCardView(value: "\(healthKit.snapshot.workouts)",
                                         label: "Workouts", valueColor: Color(hex: "22c55e"))
                                .frame(height: 80)
                        }
                        Button("Refresh") { healthKit.fetchToday() }
                            .foregroundColor(Color(hex: "6366f1"))
                    }
                }
                .padding(16)
            }
            .background(env.theme.bgPrimary.ignoresSafeArea())
            .navigationTitle("Activity")
        }
    }
}
