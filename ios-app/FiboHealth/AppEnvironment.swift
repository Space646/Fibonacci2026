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

        // Wire up: BLE pending payloads start from the current profile + snapshot
        self.bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        self.bluetooth.pendingProfile = profileStore.profile.blePayload()
        self.bluetooth.pendingSnapshot = healthKit.snapshot

        // Request HealthKit on startup
        self.healthKit.requestAuthorization()
    }

    /// Push the latest profile + health snapshot to the Pi. If already
    /// connected, writes immediately; otherwise updates the pending payloads
    /// which will be sent on next connect.
    func syncToPi() {
        bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        bluetooth.pendingProfile = profileStore.profile.blePayload()
        bluetooth.pendingSnapshot = healthKit.snapshot
        bluetooth.resync()
    }
}
