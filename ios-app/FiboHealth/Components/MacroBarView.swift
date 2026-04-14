import SwiftUI

struct MacroBarView: View {
    let label: String
    let value: Double
    let maxValue: Double
    let color: Color
    @EnvironmentObject var env: AppEnvironment

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(color)
                .frame(width: 52, alignment: .leading)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(env.theme.bgPrimary)
                        .frame(height: 5)
                    RoundedRectangle(cornerRadius: 3)
                        .fill(color)
                        .frame(width: geo.size.width * min(value / maxValue, 1.0), height: 5)
                }
            }
            .frame(height: 5)
            Text(String(format: "%.1fg", value))
                .font(.system(size: 11))
                .foregroundColor(color)
                .frame(width: 40, alignment: .trailing)
        }
        .frame(height: 20)
    }
}
