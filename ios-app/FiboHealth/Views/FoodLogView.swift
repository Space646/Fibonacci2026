import SwiftUI

struct FoodLogView: View {
    @EnvironmentObject var env: AppEnvironment
    @EnvironmentObject var healthKit: HealthKitService
    @EnvironmentObject var bluetooth: BluetoothClient

    var body: some View {
        NavigationStack {
            // Always use a List so `.refreshable` has a scrollable host in
            // both the empty and populated states.
            List {
                if bluetooth.foodLog.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "fork.knife")
                            .font(.system(size: 40))
                            .foregroundColor(env.theme.textMuted)
                        Text("No scans yet.\nScan food on your Pi device.")
                            .multilineTextAlignment(.center)
                            .font(.system(size: 14))
                            .foregroundColor(env.theme.textMuted)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 60)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                } else {
                    ForEach(bluetooth.foodLog) { entry in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(entry.foodName)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(env.theme.textPrimary)
                                Text("\(Int(entry.weightG))g · \(entry.timestamp)")
                                    .font(.system(size: 10))
                                    .foregroundColor(env.theme.textMuted)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("\(Int(entry.calories)) kcal")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(Color(hex: "06b6d4"))
                                HealthBadgeView(isHealthy: entry.isHealthy)
                            }
                        }
                        .listRowBackground(env.theme.bgSurface)
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .refreshable {
                healthKit.requestAuthorization()
                env.syncToPi()
                try? await Task.sleep(nanoseconds: 600_000_000)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(env.theme.bgPrimary.ignoresSafeArea())
            .navigationTitle("Food Log")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}
