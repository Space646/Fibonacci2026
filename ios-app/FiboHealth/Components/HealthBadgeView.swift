import SwiftUI

struct HealthBadgeView: View {
    let isHealthy: Bool
    var customText: String? = nil
    @EnvironmentObject var env: AppEnvironment

    var label: String { customText ?? (isHealthy ? "✓ Healthy" : "✗ Unhealthy") }

    var body: some View {
        Text(label)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(isHealthy ? env.theme.healthy : env.theme.unhealthy)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(isHealthy ? env.theme.healthyBg : env.theme.unhealthyBg)
            .clipShape(Capsule())
    }
}
