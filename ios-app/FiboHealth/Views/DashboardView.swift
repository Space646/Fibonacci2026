import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var env: AppEnvironment
    @EnvironmentObject var bluetooth: BluetoothClient
    @EnvironmentObject var healthKit: HealthKitService

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {

                    // Header
                    HStack {
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [Color(hex: "6366f1"), Color(hex: "06b6d4")],
                                    startPoint: .topLeading, endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 40, height: 40)
                            .overlay(
                                Text(env.profileStore.profile.name.isEmpty ? "G"
                                     : String(env.profileStore.profile.name.prefix(1)).uppercased())
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            )
                        VStack(alignment: .leading, spacing: 2) {
                            Text(env.profileStore.profile.name.isEmpty
                                 ? "Guest" : env.profileStore.profile.name)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(env.theme.textPrimary)
                            Text(Date.now.formatted(.dateTime.weekday().day().month()))
                                .font(.system(size: 10))
                                .foregroundColor(env.theme.textMuted)
                        }
                        Spacer()
                        // BLE status pill
                        HStack(spacing: 4) {
                            Circle()
                                .fill(bluetooth.isConnected ? Color(hex: "22c55e") : Color(hex: "64748b"))
                                .frame(width: 6, height: 6)
                            Text(bluetooth.isConnected ? "Pi connected" : "No Pi")
                                .font(.system(size: 10))
                                .foregroundColor(env.theme.textMuted)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(env.theme.bgSurface)
                        .cornerRadius(8)
                    }

                    // Calorie ring
                    CalorieRingView(
                        consumed: bluetooth.sessionState.caloriesConsumed,
                        goal: env.profileStore.profile.calculatedDailyGoal
                    )
                    .frame(width: 180, height: 180)

                    // Stats row
                    HStack(spacing: 8) {
                        StatCardView(
                            value: "\(Int(bluetooth.sessionState.caloriesConsumed))",
                            label: "eaten",
                            valueColor: Color(hex: "06b6d4")
                        )
                        StatCardView(
                            value: "\(Int(env.profileStore.profile.calculatedDailyGoal))",
                            label: "goal",
                            valueColor: Color(hex: "22c55e")
                        )
                        StatCardView(
                            value: "\(Int(healthKit.snapshot.caloriesBurned))",
                            label: "burned",
                            valueColor: env.theme.caloriesBurned
                        )
                    }

                    // HealthKit strip
                    VStack(alignment: .leading, spacing: 8) {
                        Text("HEALTHKIT · TODAY")
                            .font(.system(size: 9))
                            .tracking(1)
                            .foregroundColor(env.theme.textMuted)
                        HStack {
                            StatCardView(value: "\(healthKit.snapshot.steps)",
                                         label: "steps", valueColor: Color(hex: "6366f1"))
                            StatCardView(value: "\(healthKit.snapshot.activeMinutes)",
                                         label: "active min", valueColor: Color(hex: "06b6d4"))
                            StatCardView(value: "\(healthKit.snapshot.workouts)",
                                         label: "workouts", valueColor: Color(hex: "22c55e"))
                        }
                    }
                    .padding(12)
                    .background(env.theme.bgSurface)
                    .cornerRadius(10)

                    // Recent scans from Pi
                    if !bluetooth.foodLog.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("RECENT SCANS FROM PI")
                                .font(.system(size: 9))
                                .tracking(1)
                                .foregroundColor(env.theme.textMuted)

                            ForEach(bluetooth.foodLog.prefix(3)) { entry in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(entry.foodName)
                                            .font(.system(size: 13, weight: .semibold))
                                            .foregroundColor(env.theme.textPrimary)
                                        Text("\(Int(entry.weightG))g")
                                            .font(.system(size: 10))
                                            .foregroundColor(env.theme.textMuted)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("\(Int(entry.calories)) kcal")
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(Color(hex: "06b6d4"))
                                        HealthBadgeView(isHealthy: entry.isHealthy)
                                    }
                                }
                                .padding(10)
                                .background(env.theme.bgPrimary)
                                .cornerRadius(8)
                            }
                        }
                        .padding(12)
                        .background(env.theme.bgSurface)
                        .cornerRadius(10)
                    }
                }
                .padding(16)
            }
            .background(env.theme.bgPrimary.ignoresSafeArea())
            .navigationTitle("")
            .navigationBarHidden(true)
        }
    }
}
