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
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission", e)
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
                METRIC_STEPS -> {
                    val request = AggregateGroupByPeriodRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(request)
                    for (bucket in response) {
                        val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                        result[date] = steps.toFloat()
                    }
                }
                METRIC_CALORIES -> {
                    val request = AggregateGroupByPeriodRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(request)
                    for (bucket in response) {
                        val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        val kcal = bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                        result[date] = kcal.toFloat()
                    }
                }
                METRIC_DISTANCE -> {
                    val request = AggregateGroupByPeriodRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                        timeRangeSlicer = Period.ofDays(1)
                    )
                    val response = client.aggregateGroupByPeriod(request)
                    for (bucket in response) {
                        val date = bucket.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                        val meters = bucket.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                        result[date] = (meters / 1000.0).toFloat()
                    }
                }
                METRIC_SLEEP -> {
                    val request = AggregateGroupByPeriodRequest(
                        metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                        timeRangeFilter = timeRangeFilter,
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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data", e)
        }

        return result
    }
}
