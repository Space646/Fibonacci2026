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

    suspend fun fetchWeekCaloriesBurned(): List<Int> {
        val today = LocalDate.now()
        return (6 downTo 0).map { daysAgo ->
            val date  = today.minusDays(daysAgo.toLong())
            val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end   = if (daysAgo == 0) Instant.now()
                        else date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val range = TimeRangeFilter.between(start, end)
            runCatching {
                val active = client.aggregate(
                    AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), range)
                )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toFloat() ?: 0f
                val basal  = client.aggregate(
                    AggregateRequest(setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL), range)
                )[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories?.toFloat() ?: 0f
                (active + basal).toInt()
            }.getOrDefault(0)
        }
    }
}
