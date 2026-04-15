import CoreBluetooth
import Combine
import Foundation

// Must match Pi's bluetooth_server.py UUIDs exactly
private let serviceUUID       = CBUUID(string: "12345678-1234-5678-1234-56789abcdef0")
private let charUserProfile   = CBUUID(string: "12345678-1234-5678-1234-56789abcdef1")
private let charHealthSnap    = CBUUID(string: "12345678-1234-5678-1234-56789abcdef2")
private let charFoodLogSync   = CBUUID(string: "12345678-1234-5678-1234-56789abcdef3")
private let charSessionState  = CBUUID(string: "12345678-1234-5678-1234-56789abcdef4")

final class BluetoothClient: NSObject, ObservableObject {
    @Published var isConnected: Bool = false
    @Published var isScanning: Bool = false
    @Published var foodLog: [FoodLogEntry] = []
    @Published var sessionState: SessionState = .empty
    @Published var lastSyncTime: Date?

    private var central: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var characteristics: [CBUUID: CBCharacteristic] = [:]

    // Data to send on next connect (set before scan); also used by resync()
    var pendingProfile: [String: Any]?
    var pendingSnapshot: HealthSnapshot?
    var pendingDeviceId: String?

    override init() {
        super.init()
        central = CBCentralManager(delegate: self, queue: nil)
    }

    func startScanning() {
        guard central.state == .poweredOn else { return }
        isScanning = true
        central.scanForPeripherals(withServices: [serviceUUID], options: nil)
    }

    func disconnect() {
        if let p = peripheral { central.cancelPeripheralConnection(p) }
    }

    /// Re-push the current profile + snapshot to the Pi without disconnecting.
    /// Safe to call when not connected (no-op).
    func resync() {
        guard isConnected else { return }
        sendProfile()
        sendSnapshot()
    }

    // MARK: - Writes (internal so AppEnvironment/resync can trigger them)

    func sendProfile() {
        guard let char = characteristics[charUserProfile],
              var payload = pendingProfile else { return }
        // Ensure device_id is present — pendingProfile from UserProfile.blePayload
        // already includes it, but inject pendingDeviceId as a fallback.
        if payload["device_id"] == nil, let did = pendingDeviceId {
            payload["device_id"] = did
        }
        guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return }
        peripheral?.writeValue(data, for: char, type: .withResponse)
    }

    func sendSnapshot() {
        guard let char = characteristics[charHealthSnap],
              let snap = pendingSnapshot,
              let deviceId = pendingDeviceId else { return }
        let payload: [String: Any] = [
            "device_id": deviceId,
            "date": snap.date,
            "steps": snap.steps,
            "calories_burned": snap.caloriesBurned,
            "active_minutes": snap.activeMinutes,
            "workouts": snap.workouts,
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return }
        peripheral?.writeValue(data, for: char, type: .withResponse)
    }
}

// MARK: - CBCentralManagerDelegate

extension BluetoothClient: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn { startScanning() }
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral,
                        advertisementData: [String: Any], rssi RSSI: NSNumber) {
        self.peripheral = peripheral
        central.stopScan()
        isScanning = false
        central.connect(peripheral, options: nil)
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        DispatchQueue.main.async { self.isConnected = true }
        peripheral.delegate = self
        peripheral.discoverServices([serviceUUID])
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral,
                        error: Error?) {
        DispatchQueue.main.async {
            self.isConnected = false
            self.characteristics = [:]
        }
        // Auto-reconnect
        startScanning()
    }
}

// MARK: - CBPeripheralDelegate

extension BluetoothClient: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let service = peripheral.services?.first(where: { $0.uuid == serviceUUID }) else { return }
        peripheral.discoverCharacteristics(
            [charUserProfile, charHealthSnap, charFoodLogSync, charSessionState],
            for: service
        )
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        service.characteristics?.forEach { char in
            characteristics[char.uuid] = char
            if char.uuid == charFoodLogSync || char.uuid == charSessionState {
                peripheral.setNotifyValue(true, for: char)
            }
        }
        // Sync on connect
        sendProfile()
        sendSnapshot()
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard let data = characteristic.value else { return }

        if characteristic.uuid == charFoodLogSync {
            if let entries = try? JSONDecoder().decode([FoodLogEntry].self, from: data) {
                DispatchQueue.main.async { self.foodLog = entries }
            }
        } else if characteristic.uuid == charSessionState {
            if let state = try? JSONDecoder().decode(SessionState.self, from: data) {
                DispatchQueue.main.async {
                    self.sessionState = state
                    self.lastSyncTime = Date()
                }
            }
        }
    }
}
