package com.perspectivelive.wallpaper.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.LocalDate

/**
 * Manager class for storing and retrieving Health Connect data.
 * Maps a LocalDate string to a Float value.
 */
class HealthCacheManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "health_cache_prefs"
        private const val TAG = "HealthCacheManager"
    }

    /**
     * Retrieves all cached health data.
     */
    fun getHealthCache(): Map<LocalDate, Float> {
        val result = mutableMapOf<LocalDate, Float>()
        try {
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                parseAndAddEntry(key, value, result)
            }
        } catch (e: ClassCastException) {
            Log.e(TAG, "Error reading health cache", e)
        }
        return result
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
        try {
            val editor = prefs.edit()
            for ((date, value) in data) {
                editor.putFloat(date.toString(), value)
            }
            editor.apply()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error saving health cache due to security/storage issue", e)
        }
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
}
