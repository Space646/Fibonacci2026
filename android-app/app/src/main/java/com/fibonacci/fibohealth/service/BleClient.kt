package com.fibonacci.fibohealth.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.data.model.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── UUID constants (must match Pi's bluetooth_server.py) ──────────────────────
private val SERVICE_UUID     = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
private val CHAR_PROFILE     = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
private val CHAR_HEALTH_SNAP = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
private val CHAR_FOOD_LOG    = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
private val CHAR_SESSION     = UUID.fromString("12345678-1234-5678-1234-56789abcdef4")
private val CCCD_UUID        = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private const val TAG = "BleClient"
// Any single GATT operation (MTU, descriptor write, char write) that doesn't
// report a callback within this window is treated as failed so the pipeline
// doesn't stall. CoreBluetooth peripherals (bless on macOS) don't always
// surface every callback Android expects — notably onDescriptorWrite for
// CCCD writes, which CoreBluetooth intercepts internally.
private const val OP_TIMEOUT_MS = 3000L

// ── Chunk reassembler ─────────────────────────────────────────────────────────
class BleChunkReassembler {
    private val chunks = mutableMapOf<Int, ByteArray>()
    private var total = 0

    fun feed(data: ByteArray): ByteArray? {
        if (data.isEmpty() || data[0] != 0xFB.toByte()) return data // not framed
        val seq   = data[1].toInt() and 0xFF
        val tot   = data[2].toInt() and 0xFF
        total = tot
        chunks[seq] = data.drop(3).toByteArray()
        if (chunks.size < total) return null
        return (0 until total).map { chunks[it]!! }.reduce { a, b -> a + b }
            .also { chunks.clear() }
    }
}

// ── BleClient ─────────────────────────────────────────────────────────────────
@Singleton
@SuppressLint("MissingPermission")
class BleClient @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected   = MutableStateFlow(false)
    private val _isScanning    = MutableStateFlow(false)
    private val _foodLog       = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    private val _sessionState  = MutableStateFlow<SessionState?>(null)
    private val _lastSyncTime  = MutableStateFlow<Long?>(null)

    val isConnected:  StateFlow<Boolean>         = _isConnected.asStateFlow()
    val isScanning:   StateFlow<Boolean>         = _isScanning.asStateFlow()
    val foodLog:      StateFlow<List<FoodLogEntry>> = _foodLog.asStateFlow()
    val sessionState: StateFlow<SessionState?>   = _sessionState.asStateFlow()
    val lastSyncTime: StateFlow<Long?>           = _lastSyncTime.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private val foodLogReassembler   = BleChunkReassembler()
    private val sessionReassembler   = BleChunkReassembler()
    private val writeQueue           = Channel<Pair<BluetoothGattCharacteristic, ByteArray>>(Channel.BUFFERED)

    // Pending write results
    private var writeDeferred: CompletableDeferred<Boolean>? = null

    // Profile + health snapshot bytes to send on connect
    var profilePayload: ByteArray? = null
    var healthSnapPayload: ByteArray? = null

    private val scanner: BluetoothLeScanner? get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter?.bluetoothLeScanner

    // ── Scan ──────────────────────────────────────────────────────────────────
    fun startScan() {
        if (_isScanning.value || _isConnected.value) return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
        _isScanning.value = true
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    /// Re-push profile + health snapshot to the Pi without reconnecting.
    /// No-op when not connected or when the service isn't resolved yet.
    fun resync() {
        val g = gatt ?: return
        val service = g.getService(SERVICE_UUID) ?: return
        scope.launch {
            profilePayload?.let    { write(service.getCharacteristic(CHAR_PROFILE), it) }
            healthSnapPayload?.let { write(service.getCharacteristic(CHAR_HEALTH_SNAP), it) }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.i(TAG, "scan hit: ${result.device.address} rssi=${result.rssi}")
            stopScan()
            result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed: $errorCode")
            _isScanning.value = false
        }
    }

    // Guard so discoverServices runs exactly once per connection even if the
    // MTU callback fires late or the fallback timer wins the race.
    private var servicesRequested = false

    // ── GATT callback ─────────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange status=$status newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@BleClient.gatt = gatt
                    _isConnected.value = true
                    servicesRequested = false
                    // Android defaults to a 23-byte MTU; profile/snapshot JSON
                    // exceeds that and writes would fail with
                    // GATT_INVALID_ATTRIBUTE_LENGTH. iOS/CoreBluetooth
                    // auto-negotiates, Android must ask. Service discovery is
                    // deferred to onMtuChanged (or a fallback timer) so the
                    // first writes go out on the negotiated MTU.
                    val ok = gatt.requestMtu(517)
                    Log.i(TAG, "requestMtu initiated=$ok")
                    if (!ok) {
                        triggerDiscover(gatt)
                    } else {
                        // Fallback: if CoreBluetooth never reports back, don't stall.
                        scope.launch {
                            delay(OP_TIMEOUT_MS)
                            if (!servicesRequested && this@BleClient.gatt === gatt) {
                                Log.w(TAG, "onMtuChanged not received within ${OP_TIMEOUT_MS}ms; discovering anyway")
                                triggerDiscover(gatt)
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _isConnected.value = false
                    this@BleClient.gatt = null
                    servicesRequested = false
                    startScan()     // auto-reconnect
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "onMtuChanged mtu=$mtu status=$status")
            triggerDiscover(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "onServicesDiscovered status=$status services=${gatt.services.map { it.uuid }}")
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.w(TAG, "service $SERVICE_UUID not found on peripheral")
                return
            }
            scope.launch {
                enableNotify(gatt, service.getCharacteristic(CHAR_FOOD_LOG))
                enableNotify(gatt, service.getCharacteristic(CHAR_SESSION))
                profilePayload?.let    { write(service.getCharacteristic(CHAR_PROFILE), it) }
                healthSnapPayload?.let { write(service.getCharacteristic(CHAR_HEALTH_SNAP), it) }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                CHAR_FOOD_LOG -> foodLogReassembler.feed(value)?.let { bytes ->
                    runCatching {
                        _foodLog.value = json.decodeFromString<List<FoodLogEntry>>(bytes.toString(Charsets.UTF_8))
                        _lastSyncTime.value = System.currentTimeMillis()
                    }
                }
                CHAR_SESSION -> sessionReassembler.feed(value)?.let { bytes ->
                    runCatching {
                        _sessionState.value = json.decodeFromString<SessionState>(bytes.toString(Charsets.UTF_8))
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            Log.i(TAG, "onCharacteristicWrite ${characteristic.uuid} status=$status")
            writeDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int
        ) {
            Log.i(TAG, "onDescriptorWrite ${descriptor.characteristic.uuid}/${descriptor.uuid} status=$status")
            writeDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }
    }

    private fun triggerDiscover(gatt: BluetoothGatt) {
        if (servicesRequested) return
        servicesRequested = true
        val ok = gatt.discoverServices()
        Log.i(TAG, "discoverServices initiated=$ok")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun enableNotify(gatt: BluetoothGatt, char: BluetoothGattCharacteristic?) {
        if (char == null) { Log.w(TAG, "enableNotify: characteristic null"); return }
        val notifyOk = gatt.setCharacteristicNotification(char, true)
        Log.i(TAG, "setCharacteristicNotification ${char.uuid} ok=$notifyOk")
        val descriptor = char.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            // CoreBluetooth peripherals don't expose a client-writable CCCD;
            // subscriptions are handled implicitly on the peripheral side.
            Log.i(TAG, "no CCCD on ${char.uuid} — relying on peripheral-side subscribe")
            return
        }
        val deferred = CompletableDeferred<Boolean>()
        writeDeferred = deferred
        val rc = gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        Log.i(TAG, "writeDescriptor CCCD on ${char.uuid} rc=$rc")
        val completed = withTimeoutOrNull(OP_TIMEOUT_MS) { deferred.await() }
        if (completed == null) Log.w(TAG, "writeDescriptor timeout on ${char.uuid}")
        delay(30)
    }

    private suspend fun write(char: BluetoothGattCharacteristic?, data: ByteArray) {
        val g = gatt ?: return
        if (char == null) { Log.w(TAG, "write: characteristic null"); return }
        val deferred = CompletableDeferred<Boolean>()
        writeDeferred = deferred
        val rc = g.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        Log.i(TAG, "writeCharacteristic ${char.uuid} bytes=${data.size} rc=$rc")
        val ok = withTimeoutOrNull(OP_TIMEOUT_MS) { deferred.await() }
        if (ok == null) Log.w(TAG, "writeCharacteristic timeout on ${char.uuid}")
    }
}
