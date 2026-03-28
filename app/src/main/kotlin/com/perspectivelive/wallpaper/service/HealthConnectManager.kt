package com.perspectivelive.wallpaper.service

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectManager"

        const val METRIC_NONE = "NONE"
        const val METRIC_STEPS = "STEPS"
        const val METRIC_CALORIES = "CALORIES"
        const val METRIC_DISTANCE = "DISTANCE"
        const val METRIC_SLEEP = "SLEEP"

        fun getRequiredPermission(metric: String): String? {
            return when (metric) {
                METRIC_STEPS -> HealthPermission.getReadPermission(StepsRecord::class)
                METRIC_CALORIES -> HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
                METRIC_DISTANCE -> HealthPermission.getReadPermission(DistanceRecord::class)
                METRIC_SLEEP -> HealthPermission.getReadPermission(SleepSessionRecord::class)
                else -> null
            }
        }
    }

    suspend fun hasPermissions(metric: String): Boolean {
        if (metric == METRIC_NONE) return true
        val permission = getRequiredPermission(metric) ?: return true

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(permission)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Health Connect Client not available", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Error checking permission due to security", e)
            false
        }
    }

    suspend fun fetchAggregateData(
        metric: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Float> {
        val result = mutableMapOf<LocalDate, Float>()
        if (metric == METRIC_NONE) return result

        try {
            val client = HealthConnectClient.getOrCreate(context)
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.plusDays(1).atStartOfDay()
            val timeRangeFilter = TimeRangeFilter.between(startDateTime, endDateTime)

            when (metric) {
                METRIC_STEPS -> fetchSteps(client, timeRangeFilter, result)
                METRIC_CALORIES -> fetchCalories(client, timeRangeFilter, result)
                METRIC_DISTANCE -> fetchDistance(client, timeRangeFilter, result)
                METRIC_SLEEP -> fetchSleep(client, timeRangeFilter, result)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException fetching data", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException fetching data", e)
        } catch (e: android.os.RemoteException) {
            Log.e(TAG, "RemoteException fetching data", e)
        }

        return result
    }

    private suspend fun fetchSteps(client: HealthConnectClient, filter: TimeRangeFilter, result: MutableMap<LocalDate, Float>) {
        val request = AggregateGroupByPeriodRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = filter,
            timeRangeSlicer = Period.ofDays(1)
        )
        val response = client.aggregateGroupByPeriod(request)
        for (bucket in response) {
            val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
            result[date] = steps.toFloat()
        }
    }

    private suspend fun fetchCalories(client: HealthConnectClient, filter: TimeRangeFilter, result: MutableMap<LocalDate, Float>) {
        val request = AggregateGroupByPeriodRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = filter,
            timeRangeSlicer = Period.ofDays(1)
        )
        val response = client.aggregateGroupByPeriod(request)
        for (bucket in response) {
            val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val kcal = bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            result[date] = kcal.toFloat()
        }
    }

    private suspend fun fetchDistance(client: HealthConnectClient, filter: TimeRangeFilter, result: MutableMap<LocalDate, Float>) {
        val request = AggregateGroupByPeriodRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = filter,
            timeRangeSlicer = Period.ofDays(1)
        )
        val response = client.aggregateGroupByPeriod(request)
        for (bucket in response) {
            val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val meters = bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            result[date] = (meters / 1000.0).toFloat()
        }
    }

    private suspend fun fetchSleep(client: HealthConnectClient, filter: TimeRangeFilter, result: MutableMap<LocalDate, Float>) {
        val request = AggregateGroupByPeriodRequest(
            metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
            timeRangeFilter = filter,
            timeRangeSlicer = Period.ofDays(1)
        )
        val response = client.aggregateGroupByPeriod(request)
        for (bucket in response) {
            val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            val durationMillis = bucket.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMillis() ?: 0L
            result[date] = (durationMillis / (1000f * 60f * 60f))
        }
    }
}
