package com.perspectivelive.wallpaper.service

import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.modules.DayCounterModule
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.perspectivelive.wallpaper.data.HealthCacheManager

/**
 * Wallpaper service for the Day Counter feature.
 * Extends BaseWallpaperService to reuse common logic.
 */
class DayCounterService : BaseWallpaperService() {

    override fun createBaseEngine(): BaseEngine {
        return DayCounterEngine()
    }

    inner class DayCounterEngine : BaseEngine() {

        private val dayCounterModule = DayCounterModule()
        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override fun hasPreferences(): Boolean {
            return preferencesManager.hasDayCounterPreferences()
        }

        override fun getGridState(preferences: UserPreferences): GridState? {
            if (!hasPreferences()) return null

            val today = LocalDate.now()
            val totalDays = dayCounterModule.calculateTotalItems(preferences)
            val pastDays = dayCounterModule.calculatePastItems(preferences, today)
            val currentIndex = dayCounterModule.calculateCurrentItemIndex(preferences, today)
            val remainingDays = maxOf(0, totalDays - pastDays - 1)

            return GridState(
                totalItems = totalDays,
                pastItems = pastDays,
                currentIndex = currentIndex,
                futureItems = remainingDays,
                startDate = preferences.countdownStartDate ?: LocalDate.now(),
                expectedTotal = totalDays
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible && hasPreferences()) {
                val prefs = preferencesManager.getPreferences()
                if (prefs.healthMetric != HealthConnectManager.METRIC_NONE) {
                    syncHealthDataForToday(prefs.healthMetric)
                }
            }
        }

        private fun syncHealthDataForToday(metric: String) {
            serviceScope.launch {
                try {
                    val hcManager = HealthConnectManager(this@DayCounterService)
                    if (hcManager.hasPermissions(metric)) {
                        val today = LocalDate.now()
                        // Fetch only today
                        val data = hcManager.fetchAggregateData(metric, today, today)
                        val cacheManager = HealthCacheManager(this@DayCounterService)

                        // Update cache with today's value
                        data[today]?.let { value ->
                            cacheManager.updateDate(today, value)

                            // Re-initialize or refresh renderer cache
                            initializeRendererAsync()
                        }
                    }
                } catch (e: IllegalStateException) {
                    // Ignore transient errors
                }
            }
        }

        override fun performMidnightUpdate(preferences: UserPreferences) {
            // Day counter updates every day at midnight (unlike birthday logic)
            val newState = getGridState(preferences)
            if (newState != null) {
                renderer?.updateGridState(newState)
            }

            if (preferences.healthMetric != HealthConnectManager.METRIC_NONE) {
                serviceScope.launch {
                    try {
                        val hcManager = HealthConnectManager(this@DayCounterService)
                        if (hcManager.hasPermissions(preferences.healthMetric)) {
                            val yesterday = LocalDate.now().minusDays(1)
                            // Finalize yesterday's value
                            val data = hcManager.fetchAggregateData(preferences.healthMetric, yesterday, yesterday)
                            val cacheManager = HealthCacheManager(this@DayCounterService)

                            data[yesterday]?.let { value ->
                                cacheManager.updateDate(yesterday, value)
                            }

                            // Let the renderer pick up the new day's 0 value and yesterday's finalized value
                            initializeRendererAsync()
                        }
                    } catch (e: IllegalStateException) {
                        // Ignore
                    }
                }
            }
        }
    }
}
