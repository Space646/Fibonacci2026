import SwiftUI
import SwiftData

@main
struct FiboHealthApp: App {
    var body: some Scene {
        WindowGroup {
            ContentWrapperView()
        }
        .modelContainer(for: UserProfile.self)
    }
}

struct ContentWrapperView: View {
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        // AppEnvironment is created once, keyed on the injected modelContext
        InnerView(env: AppEnvironment(modelContext: modelContext))
    }
}

private struct InnerView: View {
    @StateObject var env: AppEnvironment

    var body: some View {
        RootView()
            .environmentObject(env)
            .environmentObject(env.profileStore)
            .environmentObject(env.healthKit)
            .environmentObject(env.bluetooth)
            .preferredColorScheme(env.theme == .dark ? .dark : .light)
    }
}
