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
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        RootView()
            .environmentObject(env)
            .environmentObject(env.profileStore)
            .environmentObject(env.healthKit)
            .environmentObject(env.bluetooth)
            .onAppear {
                env.theme = colorScheme == .dark ? .dark : .light
            }
            .onChange(of: colorScheme) { _, newValue in
                env.theme = newValue == .dark ? .dark : .light
            }
    }
}
