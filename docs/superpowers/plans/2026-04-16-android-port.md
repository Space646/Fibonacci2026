# FiboHealth Android Port — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android app (Kotlin + Jetpack Compose) feature-equivalent to the iOS FiboHealth app, with adaptive tablet layout, Health Connect integration, and a BLE foreground service.

**Architecture:** Single-activity MVVM + Hilt. `MainActivity` hosts a `NavHost` inside `NavigationSuiteScaffold` (auto-switches bottom nav ↔ rail by window width). Services are Hilt singletons. `BleService` is a foreground service owning `BleClient`. Per-screen ViewModels collect `StateFlow`s from services.

**Tech Stack:** Kotlin 2.0, Jetpack Compose + Material 3, Hilt 2.52, Room 2.6, Health Connect SDK 1.1.0, kotlinx.serialization 1.7, DataStore 1.1, NavigationSuiteScaffold, ListDetailPaneScaffold

**Prerequisites:** Android Studio Hedgehog+ · Android SDK 35 · API 34+ device or emulator for Health Connect

---

## File Map

```
android-app/
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/values/strings.xml
        │   ├── res/xml/health_permissions.xml
        │   └── java/com/fibonacci/fibohealth/
        │       ├── FiboHealthApp.kt
        │       ├── MainActivity.kt
        │       ├── data/model/
        │       │   ├── UserProfile.kt
        │       │   ├── FoodLogEntry.kt
        │       │   ├── HealthSnapshot.kt
        │       │   └── SessionState.kt
        │       ├── data/local/
        │       │   ├── FiboHealthDatabase.kt
        │       │   └── UserProfileDao.kt
        │       ├── data/repository/
        │       │   └── ProfileRepository.kt
        │       ├── service/
        │       │   ├── BleClient.kt
        │       │   ├── BleService.kt
        │       │   ├── HealthConnectService.kt
        │       │   └── HealthConnectFoodLogger.kt
        │       ├── di/AppModule.kt
        │       └── ui/
        │           ├── theme/{Color,Type,Theme}.kt
        │           ├── components/{CalorieRing,StatCard,HealthBadge,MacroBar}.kt
        │           ├── navigation/FiboHealthNavigation.kt
        │           ├── dashboard/{DashboardViewModel,DashboardScreen}.kt
        │           ├── foodlog/{FoodLogViewModel,FoodLogScreen,FoodLogDetailPane}.kt
        │           ├── activity/{ActivityViewModel,ActivityScreen}.kt
        │           ├── profile/{ProfileViewModel,ProfileScreen}.kt
        │           └── device/{DeviceViewModel,DeviceScreen}.kt
        └── test/java/com/fibonacci/fibohealth/
            ├── BleChunkReassemblerTest.kt
            ├── FoodLogEntryDecoderTest.kt
            ├── FoodLogDiffTest.kt
            └── CalorieCalculatorTest.kt
```

---

## Task 1: Gradle Project Scaffolding

**Files:**
- Create: `android-app/gradle/libs.versions.toml`
- Create: `android-app/settings.gradle.kts`
- Create: `android-app/build.gradle.kts`
- Create: `android-app/app/build.gradle.kts`

- [ ] **Step 1: Create version catalog** at `android-app/gradle/libs.versions.toml`

```toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
compose-bom = "2024.09.00"
hilt = "2.52"
room = "2.6.1"
health-connect = "1.1.0-rc02"
navigation = "2.8.0"
adaptive = "1.0.0"
datastore = "1.1.1"
serialization = "1.7.1"
coroutines = "1.8.1"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.0" }
nav-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
nav-suite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite" }
adaptive-layout = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout", version.ref = "adaptive" }
adaptive-navigation = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation", version.ref = "adaptive" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-nav-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
health-connect = { group = "androidx.health.connect", name = "connect-client", version.ref = "health-connect" }
datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
coroutines = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version = "4.13.2" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.0-1.0.24" }
room = { id = "androidx.room", version.ref = "room" }
```

- [ ] **Step 2: Create `android-app/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "FiboHealth"
include(":app")
```

- [ ] **Step 3: Create `android-app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

- [ ] **Step 4: Create `android-app/app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.fibonacci.fibohealth"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.fibonacci.fibohealth"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    room { schemaDirectory("$projectDir/schemas") }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.nav.compose)
    implementation(libs.nav.suite)
    implementation(libs.adaptive.layout)
    implementation(libs.adaptive.navigation)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.health.connect)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.serialization.json)
}
```

- [ ] **Step 5: Open `android-app/` in Android Studio and sync Gradle**

Confirm: Build → Sync Project with Gradle Files completes without errors.

- [ ] **Step 6: Commit**

```bash
git add android-app/
git commit -m "feat(android): Gradle project scaffolding"
```

---

## Task 2: Data Models

**Files:**
- Create: `app/src/main/java/com/fibonacci/fibohealth/data/model/UserProfile.kt`
- Create: `app/src/main/java/com/fibonacci/fibohealth/data/model/FoodLogEntry.kt`
- Create: `app/src/main/java/com/fibonacci/fibohealth/data/model/HealthSnapshot.kt`
- Create: `app/src/main/java/com/fibonacci/fibohealth/data/model/SessionState.kt`
- Create: `app/src/test/java/com/fibonacci/fibohealth/FoodLogEntryDecoderTest.kt`
- Create: `app/src/test/java/com/fibonacci/fibohealth/CalorieCalculatorTest.kt`

- [ ] **Step 1: Write failing decoder test**

```kotlin
// FoodLogEntryDecoderTest.kt
package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.data.model.FoodLogEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodLogEntryDecoderTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun `decodes full payload with macros`() {
        val raw = """{"id":1,"food_name":"Apple","weight_g":150.0,"calories":52.0,
            "is_healthy":true,"health_score":88,"timestamp":"2026-04-16T08:30:00Z",
            "protein_g":0.3,"fat_g":0.2,"sugar_g":10.4,"fiber_g":2.1}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Apple", entry.foodName)
        assertEquals(0.3f, entry.proteinG ?: 0f, 0.01f)
    }

    @Test fun `decodes legacy payload missing macros`() {
        val raw = """{"id":2,"food_name":"Salad","weight_g":300.0,"calories":90.0,
            "is_healthy":1,"health_score":70,"timestamp":"2026-04-16T12:00:00Z"}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Salad", entry.foodName)
        assertNull(entry.proteinG)
    }
}
```

- [ ] **Step 2: Run test — expect compile failure** (FoodLogEntry not yet defined)

Run: `./gradlew :app:test --tests "*.FoodLogEntryDecoderTest" -x lint`

- [ ] **Step 3: Write failing calorie calculator test**

```kotlin
// CalorieCalculatorTest.kt
package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {
    @Test fun `male moderate activity TDEE`() {
        val profile = UserProfile(
            deviceId = "test", name = "Test", age = 30,
            weightKg = 80f, heightCm = 180f, sex = "male",
            activityLevel = "moderate", dailyCalorieGoal = null
        )
        // Mifflin-St Jeor: (10×80)+(6.25×180)−(5×30)+5 = 1780 BMR → ×1.55 = 2759
        assertEquals(2759, profile.calculatedDailyGoal)
    }
}
```

- [ ] **Step 4: Create `UserProfile.kt`**

```kotlin
package com.fibonacci.fibohealth.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Entity(tableName = "user_profile")
@Serializable
data class UserProfile(
    @PrimaryKey val deviceId: String = "",
    val name: String = "",
    val age: Int = 25,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val sex: String = "male",       // "male" | "female"
    val activityLevel: String = "moderate",
    val dailyCalorieGoal: Int? = null
) {
    val calculatedDailyGoal: Int get() {
        val bmr = if (sex == "male")
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) + 5
        else
            (10 * weightKg) + (6.25f * heightCm) - (5 * age) - 161
        val multiplier = when (activityLevel) {
            "sedentary"   -> 1.2f
            "light"       -> 1.375f
            "moderate"    -> 1.55f
            "active"      -> 1.725f
            "very_active" -> 1.9f
            else          -> 1.55f
        }
        return (bmr * multiplier).roundToInt()
    }

    fun blePayload(): ByteArray = Json.encodeToString(
        mapOf(
            "device_id" to deviceId, "name" to name,
            "age" to age.toString(), "weight_kg" to weightKg.toString(),
            "height_cm" to heightCm.toString(), "sex" to sex,
            "activity_level" to activityLevel,
            "daily_calorie_goal" to (dailyCalorieGoal?.toString() ?: "null")
        )
    ).toByteArray(Charsets.UTF_8)
}
```

- [ ] **Step 5: Create `FoodLogEntry.kt`**

```kotlin
package com.fibonacci.fibohealth.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable(with = FoodLogEntrySerializer::class)
data class FoodLogEntry(
    val id: Int,
    val foodName: String,
    val weightG: Float,
    val calories: Float,
    val isHealthy: Boolean,
    val healthScore: Int,
    val timestamp: String,
    val proteinG: Float? = null,
    val fatG: Float? = null,
    val sugarG: Float? = null,
    val fiberG: Float? = null
)

// Custom serializer handles is_healthy as Int or Boolean
object FoodLogEntrySerializer : kotlinx.serialization.KSerializer<FoodLogEntry> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("FoodLogEntry")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: FoodLogEntry) {
        // Write-path not needed (Pi → phone only)
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): FoodLogEntry {
        val obj = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val isHealthy = obj["is_healthy"]?.let {
            when {
                it is JsonPrimitive && it.isString -> it.content == "true"
                it is JsonPrimitive -> it.intOrNull?.let { n -> n != 0 } ?: it.booleanOrNull ?: false
                else -> false
            }
        } ?: false
        return FoodLogEntry(
            id          = obj["id"]!!.jsonPrimitive.int,
            foodName    = obj["food_name"]!!.jsonPrimitive.content,
            weightG     = obj["weight_g"]!!.jsonPrimitive.float,
            calories    = obj["calories"]!!.jsonPrimitive.float,
            isHealthy   = isHealthy,
            healthScore = obj["health_score"]?.jsonPrimitive?.intOrNull ?: 0,
            timestamp   = obj["timestamp"]!!.jsonPrimitive.content,
            proteinG    = obj["protein_g"]?.jsonPrimitive?.floatOrNull,
            fatG        = obj["fat_g"]?.jsonPrimitive?.floatOrNull,
            sugarG      = obj["sugar_g"]?.jsonPrimitive?.floatOrNull,
            fiberG      = obj["fiber_g"]?.jsonPrimitive?.floatOrNull
        )
    }
}
```

- [ ] **Step 6: Create `HealthSnapshot.kt` and `SessionState.kt`**

```kotlin
// HealthSnapshot.kt
package com.fibonacci.fibohealth.data.model
import kotlinx.serialization.Serializable

@Serializable
data class HealthSnapshot(
    val date: String = "",
    val steps: Int = 0,
    val caloriesBurned: Float = 0f,
    val activeMinutes: Int = 0,
    val workouts: Int = 0
) {
    fun blePayload(deviceId: String): ByteArray =
        kotlinx.serialization.json.buildJsonObject {
            put("device_id",      kotlinx.serialization.json.JsonPrimitive(deviceId))
            put("date",           kotlinx.serialization.json.JsonPrimitive(date))
            put("steps",          kotlinx.serialization.json.JsonPrimitive(steps))
            put("calories_burned",kotlinx.serialization.json.JsonPrimitive(caloriesBurned))
            put("active_minutes", kotlinx.serialization.json.JsonPrimitive(activeMinutes))
            put("workouts",       kotlinx.serialization.json.JsonPrimitive(workouts))
        }.toString().toByteArray(Charsets.UTF_8)
}

// SessionState.kt
@Serializable
data class SessionState(
    @kotlinx.serialization.SerialName("calories_consumed")  val caloriesConsumed: Float = 0f,
    @kotlinx.serialization.SerialName("calories_remaining") val caloriesRemaining: Float = 0f,
    @kotlinx.serialization.SerialName("last_scan_food")     val lastScanFood: String = "",
    @kotlinx.serialization.SerialName("last_scan_kcal")     val lastScanKcal: Float = 0f
)
```

- [ ] **Step 7: Run tests**

```
./gradlew :app:test --tests "*.FoodLogEntryDecoderTest" --tests "*.CalorieCalculatorTest" -x lint
```
Expected: Both PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/
git commit -m "feat(android): data models + decoder tests"
```

---

## Task 3: Room Database + ProfileRepository

**Files:**
- Create: `data/local/UserProfileDao.kt`
- Create: `data/local/FiboHealthDatabase.kt`
- Create: `data/repository/ProfileRepository.kt`

- [ ] **Step 1: Create `UserProfileDao.kt`**

```kotlin
package com.fibonacci.fibohealth.data.local

import androidx.room.*
import com.fibonacci.fibohealth.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)
}
```

- [ ] **Step 2: Create `FiboHealthDatabase.kt`**

```kotlin
package com.fibonacci.fibohealth.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fibonacci.fibohealth.data.model.UserProfile

@Database(entities = [UserProfile::class], version = 1, exportSchema = true)
abstract class FiboHealthDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
```

- [ ] **Step 3: Create `ProfileRepository.kt`**

```kotlin
package com.fibonacci.fibohealth.data.repository

import com.fibonacci.fibohealth.data.local.UserProfileDao
import com.fibonacci.fibohealth.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val dao: UserProfileDao) {

    val profile: Flow<UserProfile> = dao.observe().map { it ?: UserProfile() }

    suspend fun save(profile: UserProfile) = dao.upsert(profile)

    suspend fun getOrDefault(): UserProfile = dao.get() ?: UserProfile()
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/
git commit -m "feat(android): Room database + ProfileRepository"
```

---

## Task 4: BLE Client + Chunk Reassembly

**Files:**
- Create: `service/BleClient.kt`
- Create: `test/.../BleChunkReassemblerTest.kt`

- [ ] **Step 1: Write failing chunk reassembly test**

```kotlin
// BleChunkReassemblerTest.kt
package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.service.BleChunkReassembler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleChunkReassemblerTest {
    @Test fun `single-chunk payload returns immediately`() {
        val r = BleChunkReassembler()
        val payload = "hello".toByteArray()
        val frame = byteArrayOf(0xFB.toByte(), 0, 1) + payload
        assertEquals("hello", r.feed(frame)?.toString(Charsets.UTF_8))
    }

    @Test fun `two chunks reassemble in order`() {
        val r = BleChunkReassembler()
        assertNull(r.feed(byteArrayOf(0xFB.toByte(), 0, 2) + "hel".toByteArray()))
        assertEquals("hello", r.feed(byteArrayOf(0xFB.toByte(), 1, 2) + "lo".toByteArray())?.toString(Charsets.UTF_8))
    }

    @Test fun `non-framed data returns as-is`() {
        val r = BleChunkReassembler()
        val raw = "[1,2,3]".toByteArray()
        assertEquals("[1,2,3]", r.feed(raw)?.toString(Charsets.UTF_8))
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```
./gradlew :app:test --tests "*.BleChunkReassemblerTest" -x lint
```

- [ ] **Step 3: Create `BleClient.kt`** with `BleChunkReassembler` and full GATT client

```kotlin
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
```

- [ ] **Step 4: Run chunk reassembly tests**

```
./gradlew :app:test --tests "*.BleChunkReassemblerTest" -x lint
```
Expected: All 3 PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/
git commit -m "feat(android): BleClient + chunk reassembly (tests green)"
```

---

## Task 5: BLE Foreground Service

**Files:**
- Create: `service/BleService.kt`

- [ ] **Step 1: Create `BleService.kt`**

```kotlin
package com.fibonacci.fibohealth.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fibonacci.fibohealth.MainActivity
import com.fibonacci.fibohealth.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

private const val CHANNEL_ID  = "ble_service"
private const val NOTIF_ID    = 1

@AndroidEntryPoint
class BleService : Service() {

    @Inject lateinit var bleClient: BleClient

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    inner class LocalBinder : Binder() { fun client() = bleClient }

    override fun onBind(intent: Intent): IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Scanning for Pi…"))
        bleClient.startScan()
        scope.launch {
            bleClient.isConnected.collectLatest { connected ->
                val text = if (connected) "Connected to FiboHealth Pi" else "Scanning for Pi…"
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification(text))
            }
        }
    }

    override fun onDestroy() { scope.cancel(); bleClient.disconnect(); super.onDestroy() }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectIntent = PendingIntent.getService(
            this, 1,
            Intent(this, BleService::class.java).setAction("DISCONNECT"),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FiboHealth")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_bluetooth)  // add a vector drawable in res/drawable/
            .setContentIntent(openIntent)
            .apply {
                if (bleClient.isConnected.value)
                    addAction(0, "Disconnect", disconnectIntent)
            }
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "DISCONNECT") bleClient.disconnect()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Bluetooth", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }
}
```

- [ ] **Step 2: Add a minimal vector drawable** at `res/drawable/ic_bluetooth.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF"
        android:pathData="M17,5.96l-5,-5h-1v7.59L6.41,4 5,5.41 10.59,11 5,16.59 6.41,18l4.59,-4.59V21h1l5,-5.04-3.99,-4L17,5.96z"/>
</vector>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): BLE foreground service"
```

---

## Task 6: Health Connect Service

**Files:**
- Create: `service/HealthConnectService.kt`

- [ ] **Step 1: Create `HealthConnectService.kt`**

```kotlin
package com.fibonacci.fibohealth.service

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fibonacci.fibohealth.data.model.HealthSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val readPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )
    val writePermissions = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class)
    )
    val allPermissions = readPermissions + writePermissions

    suspend fun hasAllPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(allPermissions)

    suspend fun fetchToday(): HealthSnapshot {
        val today = LocalDate.now()
        val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end   = Instant.now()
        val range = TimeRangeFilter.between(start, end)

        val steps = runCatching {
            client.aggregate(
                AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range)
            )[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
        }.getOrDefault(0)

        val activeCal = runCatching {
            client.aggregate(
                AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), range)
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toFloat() ?: 0f
        }.getOrDefault(0f)

        val basalCal = runCatching {
            client.aggregate(
                AggregateRequest(setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL), range)
            )[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories?.toFloat() ?: 0f
        }.getOrDefault(0f)

        val sessions = runCatching {
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records
        }.getOrDefault(emptyList())

        val activeMinutes = sessions.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }.toInt()

        return HealthSnapshot(
            date            = today.toString(),
            steps           = steps,
            caloriesBurned  = activeCal + basalCal,
            activeMinutes   = activeMinutes,
            workouts        = sessions.size
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/
git commit -m "feat(android): HealthConnectService activity reads"
```

---

## Task 7: Health Connect Food Logger

**Files:**
- Create: `service/HealthConnectFoodLogger.kt`
- Create: `test/.../FoodLogDiffTest.kt`

- [ ] **Step 1: Write failing diff test**

```kotlin
// FoodLogDiffTest.kt
package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.service.diffFoodLog
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

// Helper to make a minimal FoodLogEntry
private fun entry(id: Int) = FoodLogEntry(id, "Food$id", 100f, 50f, true, 80, "2026-04-16T00:00:00Z")

class FoodLogDiffTest {
    @Test fun `new entries are inserted`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1), entry(2)),
            existingIds = emptySet()
        )
        assertEquals(setOf(1, 2), inserts.map { it.id }.toSet())
        assertEquals(emptySet<Int>(), deletes)
    }

    @Test fun `removed entries are deleted`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1)),
            existingIds = setOf(1, 2)
        )
        assertEquals(emptyList<FoodLogEntry>(), inserts)
        assertEquals(setOf(2), deletes)
    }

    @Test fun `no change produces empty diff`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1)),
            existingIds = setOf(1)
        )
        assertEquals(emptyList<FoodLogEntry>(), inserts)
        assertEquals(emptySet<Int>(), deletes)
    }
}
```

- [ ] **Step 2: Create `HealthConnectFoodLogger.kt`** with the `diffFoodLog` top-level function and logger class

```kotlin
package com.fibonacci.fibohealth.service

import android.content.Context
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import javax.inject.Inject
import javax.inject.Singleton

// Pure function — easy to unit-test without Android
fun diffFoodLog(
    piEntries: List<FoodLogEntry>,
    existingIds: Set<Int>
): Pair<List<FoodLogEntry>, Set<Int>> {
    val piIds    = piEntries.map { it.id }.toSet()
    val inserts  = piEntries.filter { it.id !in existingIds }
    val deletes  = existingIds - piIds
    return inserts to deletes
}

@Singleton
class HealthConnectFoodLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hcService: HealthConnectService
) {
    private val mutex = Mutex()

    suspend fun reconcile(piEntries: List<FoodLogEntry>) = mutex.withLock {
        if (!hcService.hasAllPermissions()) return@withLock

        val today = LocalDate.now()
        val range = TimeRangeFilter.between(
            today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            Instant.now()
        )
        val existing = hcService.client.readRecords(
            ReadRecordsRequest(NutritionRecord::class, range)
        ).records

        // clientRecordId format: "pi_<id>"
        val existingIds = existing.mapNotNull {
            it.metadata.clientRecordId?.removePrefix("pi_")?.toIntOrNull()
        }.toSet()

        // Prune duplicates (keep first occurrence per id)
        val seen = mutableSetOf<Int>()
        val duplicateClientIds = existing.mapNotNull { r ->
            val id = r.metadata.clientRecordId?.removePrefix("pi_")?.toIntOrNull() ?: return@mapNotNull null
            if (!seen.add(id)) r.metadata.id else null
        }
        if (duplicateClientIds.isNotEmpty()) {
            hcService.client.deleteRecords(NutritionRecord::class, duplicateClientIds, emptyList())
        }

        val (inserts, deletes) = diffFoodLog(piEntries, existingIds)

        if (deletes.isNotEmpty()) {
            val deleteIds = existing
                .filter { it.metadata.clientRecordId?.removePrefix("pi_")?.toIntOrNull() in deletes }
                .map { it.metadata.id }
            hcService.client.deleteRecords(NutritionRecord::class, deleteIds, emptyList())
        }

        if (inserts.isNotEmpty()) {
            hcService.client.insertRecords(inserts.map { it.toNutritionRecord() })
        }
    }

    private fun FoodLogEntry.toNutritionRecord(): NutritionRecord {
        val mealStart = Instant.parse(timestamp)
        return NutritionRecord(
            startTime  = mealStart,
            endTime    = mealStart.plusSeconds(1),
            startZoneOffset = ZoneOffset.UTC,
            endZoneOffset   = ZoneOffset.UTC,
            energy     = Energy.kilocalories(calories.toDouble()),
            protein    = proteinG?.let { Mass.grams(it.toDouble()) },
            totalFat   = fatG?.let    { Mass.grams(it.toDouble()) },
            sugar      = sugarG?.let  { Mass.grams(it.toDouble()) },
            dietaryFiber = fiberG?.let { Mass.grams(it.toDouble()) },
            name       = foodName,
            metadata   = Metadata(clientRecordId = "pi_$id")
        )
    }

    suspend fun removeAll() = mutex.withLock {
        if (!hcService.hasAllPermissions()) return@withLock
        val range = TimeRangeFilter.between(Instant.EPOCH, Instant.now())
        val records = hcService.client.readRecords(
            ReadRecordsRequest(NutritionRecord::class, range)
        ).records.filter { it.metadata.clientRecordId?.startsWith("pi_") == true }
        if (records.isNotEmpty()) {
            hcService.client.deleteRecords(NutritionRecord::class, records.map { it.metadata.id }, emptyList())
        }
    }
}
```

- [ ] **Step 3: Run diff tests**

```
./gradlew :app:test --tests "*.FoodLogDiffTest" -x lint
```
Expected: All 3 PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/
git commit -m "feat(android): HealthConnectFoodLogger + diff tests"
```

---

## Task 8: Hilt Module + Application Class

**Files:**
- Create: `di/AppModule.kt`
- Create: `FiboHealthApp.kt`

- [ ] **Step 1: Create `AppModule.kt`**

```kotlin
package com.fibonacci.fibohealth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.fibonacci.fibohealth.data.local.FiboHealthDatabase
import com.fibonacci.fibohealth.data.local.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FiboHealthDatabase =
        Room.databaseBuilder(ctx, FiboHealthDatabase::class.java, "fibohealth.db").build()

    @Provides
    fun provideUserProfileDao(db: FiboHealthDatabase): UserProfileDao = db.userProfileDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
}
```

- [ ] **Step 2: Create `FiboHealthApp.kt`**

```kotlin
package com.fibonacci.fibohealth

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FiboHealthApp : Application()
```

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): Hilt module + Application class"
```

---

## Task 9: Material 3 Theme

**Files:**
- Create: `ui/theme/Color.kt`
- Create: `ui/theme/Type.kt`
- Create: `ui/theme/Theme.kt`

- [ ] **Step 1: Create `Color.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds
val DarkBg      = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkBorder  = Color(0xFF334155)
val LightBg     = Color(0xFFF8FAFC)

// Accent
val Indigo      = Color(0xFF6366F1)
val Cyan        = Color(0xFF06B6D4)

// Status (fixed — do not adapt to theme)
val StatusGreen = Color(0xFF22C55E)
val StatusAmber = Color(0xFFF59E0B)
val StatusRed   = Color(0xFFEF4444)
```

- [ ] **Step 2: Create `Type.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FiboTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp),
    labelSmall     = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
```

- [ ] **Step 3: Create `Theme.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary            = Indigo,
    secondary          = Cyan,
    background         = DarkBg,
    surface            = DarkSurface,
    onBackground       = Color.White,
    onSurface          = Color.White,
    onSurfaceVariant   = Color(0xFF94A3B8),
    outline            = DarkBorder
)

private val LightColors = lightColorScheme(
    primary            = Indigo,
    secondary          = Cyan,
    background         = LightBg,
    surface            = Color.White,
    onBackground       = Color(0xFF0F172A),
    onSurface          = Color(0xFF0F172A),
    onSurfaceVariant   = Color(0xFF64748B),
    outline            = Color(0xFFE2E8F0)
)

@Composable
fun FiboHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography  = FiboTypography,
        content     = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/
git commit -m "feat(android): Material 3 theme matching iOS design tokens"
```

---

## Task 10: Reusable Components

**Files:**
- Create: `ui/components/CalorieRing.kt`
- Create: `ui/components/StatCard.kt`
- Create: `ui/components/HealthBadge.kt`
- Create: `ui/components/MacroBar.kt`

- [ ] **Step 1: Create `CalorieRing.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun CalorieRing(
    remaining: Int,
    goal: Int,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 16.dp
) {
    val fraction = if (goal > 0) (1f - remaining.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(800), label = "ring")
    val color = when {
        fraction < 0.5f -> StatusGreen
        fraction < 0.75f -> Indigo
        fraction < 0.9f -> StatusAmber
        else -> StatusRed
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val rect = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            drawArc(Color.Gray.copy(alpha = 0.2f), -90f, 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
            drawArc(color, -90f, animated * 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$remaining", fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("kcal left", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 2: Create `StatCard.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(modifier.padding(4.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 3: Create `HealthBadge.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.fibonacci.fibohealth.ui.theme.StatusGreen
import com.fibonacci.fibohealth.ui.theme.StatusRed

@Composable
fun HealthBadge(isHealthy: Boolean) {
    SuggestionChip(
        onClick = {},
        label   = { Text(if (isHealthy) "Healthy" else "Unhealthy") },
        icon    = { Icon(if (isHealthy) Icons.Rounded.Check else Icons.Rounded.Close, null) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (isHealthy) StatusGreen.copy(0.15f) else StatusRed.copy(0.15f),
            labelColor     = if (isHealthy) StatusGreen else StatusRed,
            iconContentColor = if (isHealthy) StatusGreen else StatusRed
        )
    )
}
```

- [ ] **Step 4: Create `MacroBar.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun MacroBar(label: String, grams: Float?, maxGrams: Float = 50f, color: Color) {
    grams ?: return
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("${grams}g", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress    = { (grams / maxGrams).coerceIn(0f, 1f) },
            modifier    = Modifier.fillMaxWidth().height(4.dp),
            color       = color,
            trackColor  = MaterialTheme.colorScheme.outline
        )
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/
git commit -m "feat(android): reusable Compose components"
```

---

## Task 11: Adaptive Navigation

**Files:**
- Create: `ui/navigation/FiboHealthNavigation.kt`

- [ ] **Step 1: Create `FiboHealthNavigation.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*

enum class Destination(val label: String, val icon: ImageVector, val route: String) {
    Home    ("Home",    Icons.Rounded.Home,           "home"),
    Log     ("Log",     Icons.Rounded.RestaurantMenu, "log"),
    Activity("Activity",Icons.Rounded.Bolt,           "activity"),
    Profile ("Profile", Icons.Rounded.Person,         "profile"),
    Device  ("Device",  Icons.Rounded.Bluetooth,      "device"),
}

@Composable
fun FiboHealthNavigation() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { dest ->
                item(
                    selected = currentRoute == dest.route,
                    onClick  = {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon  = { Icon(dest.icon, dest.label) },
                    label = { Text(dest.label) }
                )
            }
        }
    ) {
        NavHost(navController, startDestination = Destination.Home.route) {
            composable(Destination.Home.route)     { DashboardScreen() }
            composable(Destination.Log.route)      { FoodLogScreen() }
            composable(Destination.Activity.route) { ActivityScreen() }
            composable(Destination.Profile.route)  { ProfileScreen() }
            composable(Destination.Device.route)   { DeviceScreen() }
        }
    }
}
```

*Note:* The screen composables referenced here are created in Tasks 12–16. Android Studio will show red errors until those tasks are complete — this is expected.

- [ ] **Step 2: Commit**

```bash
git add app/src/
git commit -m "feat(android): adaptive navigation scaffold"
```

---

## Task 12: Dashboard Screen

**Files:**
- Create: `ui/dashboard/DashboardViewModel.kt`
- Create: `ui/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Create `DashboardViewModel.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.*
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val profile: UserProfile = UserProfile(),
    val sessionState: SessionState? = null,
    val recentScans: List<FoodLogEntry> = emptyList(),
    val isConnected: Boolean = false,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val bleClient: BleClient
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        profileRepo.profile,
        bleClient.sessionState,
        bleClient.foodLog,
        bleClient.isConnected,
        bleClient.lastSyncTime
    ) { profile, session, log, connected, syncTime ->
        DashboardUiState(profile, session, log.takeLast(3), connected, syncTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
```

- [ ] **Step 2: Create `DashboardScreen.kt`**

Port the iOS `DashboardView` to Compose. Mirror the structure: greeting header, BLE status pill, calorie ring, 3-column stats strip, recent scans list. Use `collectAsStateWithLifecycle()` to collect the ViewModel state.

```kotlin
package com.fibonacci.fibohealth.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.components.*
import com.fibonacci.fibohealth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val goal = state.profile.dailyCalorieGoal ?: state.profile.calculatedDailyGoal
    val remaining = (goal - (state.sessionState?.caloriesConsumed ?: 0f)).toInt()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Good morning, ${state.profile.name}",
                style = MaterialTheme.typography.headlineMedium)
        }
        item {  // BLE status pill
            SuggestionChip(
                onClick = {},
                label   = { Text(if (state.isConnected) "Pi Connected" else "Pi Disconnected") },
                colors  = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (state.isConnected) StatusGreen.copy(0.15f)
                                     else MaterialTheme.colorScheme.surface
                )
            )
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CalorieRing(remaining = remaining.coerceAtLeast(0), goal = goal)
            }
        }
        item {  // Stats strip
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${state.profile.calculatedDailyGoal}", "Goal kcal", Indigo, Modifier.weight(1f))
                StatCard("${(state.sessionState?.caloriesConsumed ?: 0f).toInt()}", "Eaten", Cyan, Modifier.weight(1f))
                StatCard("${remaining.coerceAtLeast(0)}", "Left", StatusGreen, Modifier.weight(1f))
            }
        }
        if (state.recentScans.isNotEmpty()) {
            item { Text("Recent Scans", style = MaterialTheme.typography.titleLarge) }
            items(state.recentScans) { entry ->
                ListItem(
                    headlineContent   = { Text(entry.foodName) },
                    supportingContent = { Text("${entry.weightG}g · ${entry.calories.toInt()} kcal") },
                    trailingContent   = { HealthBadge(entry.isHealthy) }
                )
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): Dashboard screen + ViewModel"
```

---

## Task 13: Food Log Screen (with Adaptive Two-Pane)

**Files:**
- Create: `ui/foodlog/FoodLogViewModel.kt`
- Create: `ui/foodlog/FoodLogDetailPane.kt`
- Create: `ui/foodlog/FoodLogScreen.kt`

- [ ] **Step 1: Create `FoodLogViewModel.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.foodlog

import androidx.lifecycle.ViewModel
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class FoodLogViewModel @Inject constructor(bleClient: BleClient) : ViewModel() {
    val foodLog: StateFlow<List<FoodLogEntry>> = bleClient.foodLog
}
```

- [ ] **Step 2: Create `FoodLogDetailPane.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.foodlog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import com.fibonacci.fibohealth.ui.components.*
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun FoodLogDetailPane(entry: FoodLogEntry, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(entry.foodName, style = MaterialTheme.typography.headlineMedium)
        Text("${entry.weightG}g · ${entry.timestamp.take(10)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("${entry.calories.toInt()}", "Calories", Indigo, Modifier.weight(1f))
            HealthBadge(entry.isHealthy)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Macros", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                MacroBar("Protein", entry.proteinG, color = Indigo)
                MacroBar("Fat",     entry.fatG,     color = StatusAmber)
                MacroBar("Sugar",   entry.sugarG,   color = Cyan)
                MacroBar("Fiber",   entry.fiberG,   color = StatusGreen)
            }
        }
    }
}
```

- [ ] **Step 3: Create `FoodLogScreen.kt`** with adaptive list+detail

```kotlin
package com.fibonacci.fibohealth.ui.foodlog

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.FoodLogEntry

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FoodLogScreen(vm: FoodLogViewModel = hiltViewModel()) {
    val log by vm.foodLog.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<FoodLogEntry>()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value     = navigator.scaffoldValue,
        listPane  = {
            AnimatedPane {
                if (log.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No food scans yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(log) { entry ->
                            ListItem(
                                headlineContent   = { Text(entry.foodName) },
                                supportingContent = { Text("${entry.weightG}g · ${entry.calories.toInt()} kcal") },
                                modifier          = Modifier.clickable {
                                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, entry)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.content?.let { entry ->
                    FoodLogDetailPane(entry, Modifier.fillMaxSize())
                }
            }
        }
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/
git commit -m "feat(android): Food Log screen with adaptive two-pane"
```

---

## Task 14: Activity Screen

**Files:**
- Create: `ui/activity/ActivityViewModel.kt`
- Create: `ui/activity/ActivityScreen.kt`

- [ ] **Step 1: Create `ActivityViewModel.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.HealthSnapshot
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val snapshot: HealthSnapshot = HealthSnapshot(),
    val hasPermission: Boolean = false,
    val loading: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val hcService: HealthConnectService
) : ViewModel() {

    private val _state = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _state.asStateFlow()
    val permissions get() = hcService.allPermissions   // passed to permission launcher

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val hasPerm = hcService.hasAllPermissions()
        if (hasPerm) {
            val snapshot = runCatching { hcService.fetchToday() }.getOrDefault(HealthSnapshot())
            _state.value = ActivityUiState(snapshot, true, false)
        } else {
            _state.value = ActivityUiState(hasPermission = false, loading = false)
        }
    }
}
```

- [ ] **Step 2: Create `ActivityScreen.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.activity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.components.StatCard
import com.fibonacci.fibohealth.ui.theme.*

@Composable
fun ActivityScreen(vm: ActivityViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val permLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    ) { vm.refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium)
        if (!state.hasPermission) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Health Connect access required",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        permLauncher.launch(vm.permissions)
                    }) { Text("Grant Access") }
                }
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { StatCard("${state.snapshot.steps}", "Steps", Cyan, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.activeMinutes}m", "Active", StatusGreen, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.caloriesBurned.toInt()}", "Burned kcal", StatusAmber, Modifier.fillMaxWidth()) }
                item { StatCard("${state.snapshot.workouts}", "Workouts", Indigo, Modifier.fillMaxWidth()) }
            }
        }
    }
}
```

*Note:* The `permLauncher` permission set should be injected from `HealthConnectService.allPermissions`. Refactor `ActivityViewModel` to expose it as a property and pass it to the launcher on click.

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): Activity screen + ViewModel"
```

---

## Task 15: Profile Screen

**Files:**
- Create: `ui/profile/ProfileViewModel.kt`
- Create: `ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Create `ProfileViewModel.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.data.model.UserProfile
import com.fibonacci.fibohealth.data.repository.ProfileRepository
import com.fibonacci.fibohealth.service.BleClient
import com.fibonacci.fibohealth.service.HealthConnectFoodLogger
import com.fibonacci.fibohealth.service.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val HC_LOGGING_KEY = booleanPreferencesKey("hc_food_logging_enabled")

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val bleClient: BleClient,
    private val hcService: HealthConnectService,
    private val hcLogger: HealthConnectFoodLogger,
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) : ViewModel() {

    val profile: StateFlow<UserProfile> =
        profileRepo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val hcLoggingEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[HC_LOGGING_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun save(profile: UserProfile) = viewModelScope.launch {
        profileRepo.save(profile)
        bleClient.profilePayload = profile.blePayload()
    }

    fun setHcLogging(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[HC_LOGGING_KEY] = enabled }
    }

    fun removeAllHcEntries() = viewModelScope.launch {
        hcLogger.removeAll()
    }
}
```

- [ ] **Step 2: Create `ProfileScreen.kt`**

Port the iOS `ProfileView` form to Compose. Use `OutlinedTextField` for text inputs, `DropdownMenuBox` for sex/activity level pickers, a `Switch` for the HC logging toggle, and a `Button` for Save and a destructive `TextButton` for Remove All.

```kotlin
package com.fibonacci.fibohealth.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.data.model.UserProfile
import com.fibonacci.fibohealth.ui.theme.StatusRed

@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val profile     by vm.profile.collectAsStateWithLifecycle()
    val hcEnabled   by vm.hcLoggingEnabled.collectAsStateWithLifecycle()
    var draft       by remember(profile) { mutableStateOf(profile) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(draft.name, { draft = draft.copy(name = it) },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.age.toString(), { draft = draft.copy(age = it.toIntOrNull() ?: draft.age) },
            label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.weightKg.toString(), { draft = draft.copy(weightKg = it.toFloatOrNull() ?: draft.weightKg) },
            label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.heightCm.toString(), { draft = draft.copy(heightCm = it.toFloatOrNull() ?: draft.heightCm) },
            label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())

        // Sex picker
        var sexExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(sexExpanded, { sexExpanded = it }) {
            OutlinedTextField(draft.sex, {}, readOnly = true, label = { Text("Sex") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sexExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(sexExpanded, { sexExpanded = false }) {
                listOf("male", "female").forEach { opt ->
                    DropdownMenuItem({ Text(opt) }, { draft = draft.copy(sex = opt); sexExpanded = false })
                }
            }
        }

        // Activity level picker
        var actExpanded by remember { mutableStateOf(false) }
        val actLevels = listOf("sedentary","light","moderate","active","very_active")
        ExposedDropdownMenuBox(actExpanded, { actExpanded = it }) {
            OutlinedTextField(draft.activityLevel, {}, readOnly = true, label = { Text("Activity Level") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(actExpanded, { actExpanded = false }) {
                actLevels.forEach { opt ->
                    DropdownMenuItem({ Text(opt) }, { draft = draft.copy(activityLevel = opt); actExpanded = false })
                }
            }
        }

        Text("Calculated daily goal: ${draft.calculatedDailyGoal} kcal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // HC logging toggle
        ListItem(
            headlineContent   = { Text("Log Food to Health Connect") },
            trailingContent   = { Switch(hcEnabled, { vm.setHcLogging(it) }) }
        )
        HorizontalDivider()

        Button({ vm.save(draft) }, Modifier.fillMaxWidth()) { Text("Save & Sync to Pi") }

        TextButton({ showRemoveDialog = true }, Modifier.fillMaxWidth()) {
            Text("Remove FiboHealth Food Entries from Health", color = StatusRed)
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove food entries?") },
            text  = { Text("This will delete all FiboHealth food entries from Health Connect.") },
            confirmButton = {
                TextButton({ vm.removeAllHcEntries(); showRemoveDialog = false }) {
                    Text("Remove", color = StatusRed)
                }
            },
            dismissButton = { TextButton({ showRemoveDialog = false }) { Text("Cancel") } }
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): Profile screen + ViewModel"
```

---

## Task 16: Device Screen

**Files:**
- Create: `ui/device/DeviceViewModel.kt`
- Create: `ui/device/DeviceScreen.kt`

- [ ] **Step 1: Create `DeviceViewModel.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fibonacci.fibohealth.service.BleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DeviceUiState(
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val lastSyncTime: Long? = null
)

@HiltViewModel
class DeviceViewModel @Inject constructor(private val bleClient: BleClient) : ViewModel() {

    val uiState: StateFlow<DeviceUiState> = combine(
        bleClient.isConnected, bleClient.isScanning, bleClient.lastSyncTime
    ) { connected, scanning, sync -> DeviceUiState(connected, scanning, sync) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceUiState())

    fun scan() = bleClient.startScan()
    fun disconnect() = bleClient.disconnect()
}
```

- [ ] **Step 2: Create `DeviceScreen.kt`**

```kotlin
package com.fibonacci.fibohealth.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fibonacci.fibohealth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceScreen(vm: DeviceViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Icon(
            if (state.isConnected) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (state.isConnected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (state.isConnected) "Connected to FiboHealth Pi"
            else if (state.isScanning) "Searching…"
            else "Not Connected",
            style = MaterialTheme.typography.titleLarge
        )
        state.lastSyncTime?.let {
            Text(
                "Last sync: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            state.isConnected -> OutlinedButton({ vm.disconnect() }) { Text("Disconnect") }
            state.isScanning  -> CircularProgressIndicator()
            else              -> Button({ vm.scan() }) { Text("Scan for Pi") }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/
git commit -m "feat(android): Device screen + ViewModel"
```

---

## Task 17: AndroidManifest, MainActivity, and Final Wiring

**Files:**
- Create: `AndroidManifest.xml`
- Create: `MainActivity.kt`
- Create: `res/values/strings.xml`
- Create: `res/xml/health_permissions.xml`

- [ ] **Step 1: Create `res/xml/health_permissions.xml`**

Required by Health Connect SDK to declare permissions in the Play Store listing.

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <uses-permission android:name="android.permission.health.READ_STEPS"/>
    <uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>
    <uses-permission android:name="android.permission.health.READ_BASAL_METABOLIC_RATE"/>
    <uses-permission android:name="android.permission.health.READ_EXERCISE"/>
    <uses-permission android:name="android.permission.health.WRITE_NUTRITION"/>
</permissions>
```

- [ ] **Step 2: Create `res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">FiboHealth</string>
    <string name="ble_permission_rationale">FiboHealth connects to your Fibonacci Pi device over Bluetooth.</string>
    <string name="hc_read_rationale">FiboHealth reads your activity data to calculate remaining daily calories.</string>
    <string name="hc_write_rationale">FiboHealth logs food scanned on your Pi as nutrition data in Health Connect.</string>
</resources>
```

- [ ] **Step 3: Create `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Bluetooth -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>

    <!-- Health Connect -->
    <uses-permission android:name="android.permission.health.READ_STEPS"/>
    <uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED"/>
    <uses-permission android:name="android.permission.health.READ_BASAL_METABOLIC_RATE"/>
    <uses-permission android:name="android.permission.health.READ_EXERCISE"/>
    <uses-permission android:name="android.permission.health.WRITE_NUTRITION"/>

    <application
        android:name=".FiboHealthApp"
        android:label="@string/app_name"
        android:theme="@style/Theme.FiboHealth">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
            <!-- Required by Health Connect for privacy policy display -->
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"/>
            </intent-filter>
            <meta-data
                android:name="health_permissions"
                android:resource="@xml/health_permissions"/>
        </activity>

        <service
            android:name=".service.BleService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false"/>

    </application>
</manifest>
```

- [ ] **Step 4: Create `MainActivity.kt`**

```kotlin
package com.fibonacci.fibohealth

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.fibonacci.fibohealth.service.BleService
import com.fibonacci.fibohealth.ui.navigation.FiboHealthNavigation
import com.fibonacci.fibohealth.ui.theme.FiboHealthTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {}
        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start BleService as foreground
        ContextCompat.startForegroundService(this, Intent(this, BleService::class.java))
        bindService(Intent(this, BleService::class.java), serviceConnection, BIND_AUTO_CREATE)

        setContent {
            FiboHealthTheme {
                FiboHealthNavigation()
            }
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }
}
```

- [ ] **Step 5: Add a minimal `res/values/themes.xml`** (required by the manifest theme reference)

```xml
<resources>
    <style name="Theme.FiboHealth" parent="android:Theme.Material.NoTitleBar"/>
</resources>
```

- [ ] **Step 6: Build and run**

```
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. Install on a device/emulator and confirm the 5-tab navigation renders.

- [ ] **Step 7: Commit**

```bash
git add app/src/ app/
git commit -m "feat(android): AndroidManifest, MainActivity, full app wiring"
```

---

## Task 18: Run All Tests

- [ ] **Step 1: Run full test suite**

```
./gradlew :app:test -x lint
```

Expected output — all 4 test classes passing:
```
FoodLogEntryDecoderTest > decodes full payload with macros PASSED
FoodLogEntryDecoderTest > decodes legacy payload missing macros PASSED
CalorieCalculatorTest > male moderate activity TDEE PASSED
BleChunkReassemblerTest > single-chunk payload returns immediately PASSED
BleChunkReassemblerTest > two chunks reassemble in order PASSED
BleChunkReassemblerTest > non-framed data returns as-is PASSED
FoodLogDiffTest > new entries are inserted PASSED
FoodLogDiffTest > removed entries are deleted PASSED
FoodLogDiffTest > no change produces empty diff PASSED
```

- [ ] **Step 2: Final commit**

```bash
git add .
git commit -m "test(android): all unit tests green"
```
