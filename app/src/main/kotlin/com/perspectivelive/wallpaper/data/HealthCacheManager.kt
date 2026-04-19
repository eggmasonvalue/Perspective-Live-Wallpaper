package com.perspectivelive.wallpaper.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.perspectivelive.wallpaper.service.HealthConnectManager
import java.time.LocalDate

data class HealthCacheSnapshot(
    val metric: String = HealthConnectManager.METRIC_NONE,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
    val lastRefreshEpochMs: Long = 0L,
    val data: Map<LocalDate, Float> = emptyMap()
)

/**
 * Manager class for storing and retrieving Health Connect data.
 * Maps a LocalDate string to a Float value.
 */
class HealthCacheManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "health_cache_prefs"
        private const val TAG = "HealthCacheManager"
        private const val KEY_META_METRIC = "__meta_metric"
        private const val KEY_META_RANGE_START = "__meta_range_start"
        private const val KEY_META_RANGE_END = "__meta_range_end"
        private const val KEY_META_LAST_REFRESH = "__meta_last_refresh"
    }

    /**
     * Retrieves all cached health data.
     */
    fun getHealthCache(): Map<LocalDate, Float> {
        return getSnapshot().data
    }

    fun getSnapshot(): HealthCacheSnapshot {
        val result = mutableMapOf<LocalDate, Float>()
        try {
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                parseAndAddEntry(key, value, result)
            }
        } catch (e: ClassCastException) {
            Log.e(TAG, "Error reading health cache", e)
        }

        val metric = prefs.getString(KEY_META_METRIC, HealthConnectManager.METRIC_NONE)
            ?: HealthConnectManager.METRIC_NONE
        val rangeStart = prefs.getString(KEY_META_RANGE_START, null)?.let(::parseDateOrNull)
        val rangeEnd = prefs.getString(KEY_META_RANGE_END, null)?.let(::parseDateOrNull)
        val lastRefreshEpochMs = try {
            prefs.getLong(KEY_META_LAST_REFRESH, 0L)
        } catch (e: ClassCastException) {
            Log.w(TAG, "Invalid health cache refresh metadata", e)
            0L
        }

        return HealthCacheSnapshot(
            metric = metric,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            lastRefreshEpochMs = lastRefreshEpochMs,
            data = result
        )
    }

    private fun parseAndAddEntry(key: String, value: Any?, result: MutableMap<LocalDate, Float>) {
        if (value is Float) {
            try {
                val date = LocalDate.parse(key)
                result[date] = value
            } catch (e: java.time.format.DateTimeParseException) {
                Log.w(TAG, "Invalid date format in cache: $key", e)
            }
        }
    }

    /**
     * Saves a batch of health data to the cache.
     */
    fun saveHealthCache(data: Map<LocalDate, Float>) {
        saveSnapshot(
            HealthCacheSnapshot(
                data = data,
                rangeStart = data.keys.minOrNull(),
                rangeEnd = data.keys.maxOrNull()
            ),
            replace = true
        )
    }

    fun saveSnapshot(snapshot: HealthCacheSnapshot, replace: Boolean = false) {
        try {
            val editor = prefs.edit()
            if (replace) {
                editor.clear()
            }

            for ((date, value) in snapshot.data) {
                editor.putFloat(date.toString(), value)
            }

            editor.putString(KEY_META_METRIC, snapshot.metric)
            snapshot.rangeStart?.let { editor.putString(KEY_META_RANGE_START, it.toString()) }
                ?: editor.remove(KEY_META_RANGE_START)
            snapshot.rangeEnd?.let { editor.putString(KEY_META_RANGE_END, it.toString()) }
                ?: editor.remove(KEY_META_RANGE_END)
            editor.putLong(KEY_META_LAST_REFRESH, snapshot.lastRefreshEpochMs)
            editor.apply()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error saving health cache due to security/storage issue", e)
        }
    }

    fun upsertSnapshotData(
        metric: String,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        lastRefreshEpochMs: Long,
        data: Map<LocalDate, Float>
    ) {
        val existing = getSnapshot()
        val replacingMetric = existing.metric != HealthConnectManager.METRIC_NONE && existing.metric != metric
        val mergedRangeStart = if (replacingMetric) {
            rangeStart
        } else {
            minDate(existing.rangeStart, rangeStart)
        }
        val mergedRangeEnd = if (replacingMetric) {
            rangeEnd
        } else {
            maxDate(existing.rangeEnd, rangeEnd)
        }

        saveSnapshot(
            HealthCacheSnapshot(
                metric = metric,
                rangeStart = mergedRangeStart,
                rangeEnd = mergedRangeEnd,
                lastRefreshEpochMs = lastRefreshEpochMs,
                data = data
            ),
            replace = replacingMetric
        )
    }

    /**
     * Updates or inserts a single value.
     */
    fun updateDate(date: LocalDate, value: Float) {
        prefs.edit().putFloat(date.toString(), value).apply()
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    private fun parseDateOrNull(value: String): LocalDate? {
        return try {
            LocalDate.parse(value)
        } catch (e: java.time.format.DateTimeParseException) {
            Log.w(TAG, "Invalid health cache date metadata: $value", e)
            null
        }
    }

    private fun minDate(first: LocalDate?, second: LocalDate): LocalDate {
        return if (first == null || second.isBefore(first)) second else first
    }

    private fun maxDate(first: LocalDate?, second: LocalDate): LocalDate {
        return if (first == null || second.isAfter(first)) second else first
    }
}
