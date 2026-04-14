import SwiftUI
import SwiftData

@MainActor
final class AppEnvironment: ObservableObject {
    @Published var theme: Theme = .dark

    let profileStore: UserProfileStore
    let healthKit: HealthKitService
    let bluetooth: BluetoothClient

    init(modelContext: ModelContext) {
        self.profileStore = UserProfileStore(modelContext: modelContext)
        self.healthKit = HealthKitService()
        self.bluetooth = BluetoothClient()

        // Wire up: when profile or health data changes, update BLE pending payloads
        self.bluetooth.pendingProfile = profileStore.profile.blePayload()
        self.bluetooth.pendingSnapshot = healthKit.snapshot

        // Request HealthKit on startup
        self.healthKit.requestAuthorization()
    }

    func syncToPi() {
        bluetooth.pendingProfile = profileStore.profile.blePayload()
        bluetooth.pendingSnapshot = healthKit.snapshot
        if bluetooth.isConnected {
            // Trigger a re-connect write by disconnecting and reconnecting
            // (or call sendProfile/sendSnapshot directly if exposed)
        }
    }
}
