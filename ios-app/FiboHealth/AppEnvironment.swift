import SwiftUI
import SwiftData
import Combine

@MainActor
final class AppEnvironment: ObservableObject {
    @Published var theme: Theme = .dark

    let profileStore: UserProfileStore
    let healthKit: HealthKitService
    let healthKitFoodLogger: HealthKitFoodLogger
    let bluetooth: BluetoothClient

    static let healthLoggingEnabledKey = "healthkit_food_logging_enabled"

    private var cancellables = Set<AnyCancellable>()

    init(modelContext: ModelContext) {
        self.profileStore = UserProfileStore(modelContext: modelContext)
        self.healthKit = HealthKitService()
        self.healthKitFoodLogger = HealthKitFoodLogger()
        self.bluetooth = BluetoothClient()

        self.bluetooth.pendingDeviceId = profileStore.profile.deviceId.uuidString
        self.bluetooth.pendingProfile = profileStore.profile.blePayload()
        self.bluetooth.pendingSnapshot = healthKit.snapshot

        self.healthKit.requestAuthorization()

        self.bluetooth.$foodLog
            .receive(on: DispatchQueue.main)
            .sink { [weak self] entries in
                self?.reconcileIfEnabled(entries: entries)
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.handleToggleChangeIfNeeded()
            }
            .store(in: &cancellables)
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

    private var lastSeenToggle: Bool = UserDefaults.standard
        .bool(forKey: AppEnvironment.healthLoggingEnabledKey)

    private func reconcileIfEnabled(entries: [FoodLogEntry]) {
        guard UserDefaults.standard.bool(forKey: Self.healthLoggingEnabledKey) else { return }
        Task { await healthKitFoodLogger.reconcile(with: entries) }
    }

    private func handleToggleChangeIfNeeded() {
        let now = UserDefaults.standard.bool(forKey: Self.healthLoggingEnabledKey)
        guard now != lastSeenToggle else { return }
        lastSeenToggle = now
        if now {
            let entries = bluetooth.foodLog
            Task {
                await healthKitFoodLogger.requestWriteAuthorization()
                await healthKitFoodLogger.reconcile(with: entries)
            }
        }
    }
}
