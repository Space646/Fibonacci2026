import SwiftUI

struct DeviceView: View {
    @EnvironmentObject var env: AppEnvironment
    @EnvironmentObject var bluetooth: BluetoothClient

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // Connection status
                VStack(spacing: 8) {
                    Circle()
                        .fill(bluetooth.isConnected ? Color(hex: "22c55e") : Color(hex: "334155"))
                        .frame(width: 60, height: 60)
                        .overlay(
                            Image(systemName: bluetooth.isConnected
                                  ? "antenna.radiowaves.left.and.right"
                                  : "antenna.radiowaves.left.and.right.slash")
                                .foregroundColor(.white)
                                .font(.system(size: 22))
                        )
                    Text(bluetooth.isConnected ? "Pi Connected" : "Not Connected")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(env.theme.textPrimary)
                    if let syncTime = bluetooth.lastSyncTime {
                        Text("Last sync: \(syncTime.formatted(.relative(presentation: .named)))")
                            .font(.system(size: 12))
                            .foregroundColor(env.theme.textMuted)
                    }
                }
                .padding(20)
                .background(env.theme.bgSurface)
                .cornerRadius(16)

                if bluetooth.isConnected {
                    Button("Disconnect") { bluetooth.disconnect() }
                        .foregroundColor(Color(hex: "f87171"))
                        .padding()
                        .background(env.theme.bgSurface)
                        .cornerRadius(10)
                } else if bluetooth.isScanning {
                    HStack(spacing: 10) {
                        ProgressView()
                        Text("Searching for Pi…")
                            .foregroundColor(env.theme.textPrimary)
                    }
                    .padding()
                    .background(env.theme.bgSurface)
                    .cornerRadius(10)
                } else {
                    Button("Scan for Pi") {
                        bluetooth.startScanning()
                    }
                    .foregroundColor(Color(hex: "6366f1"))
                    .padding()
                    .background(env.theme.bgSurface)
                    .cornerRadius(10)
                }

                Text("Make sure your Pi device is powered on and nearby.")
                    .font(.system(size: 12))
                    .foregroundColor(env.theme.textMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                Spacer()
            }
            .padding(20)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(env.theme.bgPrimary.ignoresSafeArea())
            .navigationTitle("Device")
        }
    }
}
