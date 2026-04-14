import SwiftUI

struct StatCardView: View {
    let value: String
    let label: String
    let valueColor: Color
    @EnvironmentObject var env: AppEnvironment

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(valueColor)
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(env.theme.textMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(env.theme.bgSurface)
        .cornerRadius(8)
    }
}
