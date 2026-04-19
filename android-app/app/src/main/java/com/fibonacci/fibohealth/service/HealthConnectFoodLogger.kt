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
            metadata   = Metadata.manualEntry(clientRecordId = "pi_$id")
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
