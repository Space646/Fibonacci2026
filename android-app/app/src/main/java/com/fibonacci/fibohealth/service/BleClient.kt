package com.fibonacci.fibohealth.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.data.model.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
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

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    // ── GATT callback ─────────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@BleClient.gatt = gatt
                    _isConnected.value = true
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _isConnected.value = false
                    this@BleClient.gatt = null
                    startScan()     // auto-reconnect
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(SERVICE_UUID) ?: return
            scope.launch {
                // Subscribe to food log + session notifications
                enableNotify(gatt, service.getCharacteristic(CHAR_FOOD_LOG))
                enableNotify(gatt, service.getCharacteristic(CHAR_SESSION))
                // Push profile + health snapshot
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
        ) { writeDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS) }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int
        ) { writeDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private suspend fun enableNotify(gatt: BluetoothGatt, char: BluetoothGattCharacteristic?) {
        char ?: return
        gatt.setCharacteristicNotification(char, true)
        val descriptor = char.getDescriptor(CCCD_UUID) ?: return
        val deferred = CompletableDeferred<Boolean>()
        writeDeferred = deferred
        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        deferred.await()
        delay(30)
    }

    private suspend fun write(char: BluetoothGattCharacteristic?, data: ByteArray) {
        val g = gatt ?: return
        char ?: return
        val deferred = CompletableDeferred<Boolean>()
        writeDeferred = deferred
        g.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        deferred.await()
    }
}
