import SwiftUI

struct RootView: View {
    @EnvironmentObject var env: AppEnvironment

    var body: some View {
        TabView {
            DashboardView()
                .tabItem { Label("Home", systemImage: "house.fill") }
            FoodLogView()
                .tabItem { Label("Log", systemImage: "list.bullet") }
            ActivityView()
                .tabItem { Label("Activity", systemImage: "figure.walk") }
            ProfileView()
                .tabItem { Label("Profile", systemImage: "person.fill") }
            DeviceView()
                .tabItem { Label("Device", systemImage: "antenna.radiowaves.left.and.right") }
        }
        .background(env.theme.bgPrimary)
        .accentColor(Color(hex: "6366f1"))
    }
}
