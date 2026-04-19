package com.perspectivelive.wallpaper.service

import android.content.SharedPreferences
import android.util.Log
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.HealthCacheManager
import com.perspectivelive.wallpaper.data.HealthCacheSnapshot
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.modules.DayCounterModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Wallpaper service for the Day Counter feature.
 * Extends BaseWallpaperService to reuse common lifecycle and rendering behavior.
 */
class DayCounterService : BaseWallpaperService() {

    companion object {
        private const val TAG = "DayCounterService"
        private const val HEALTH_REFRESH_INTERVAL_MS = 15 * 60 * 1000L
        private val HEALTH_RELOAD_ONLY_KEYS = setOf(
            "health_goal",
            "show_stat_overlay"
        )
        private val HEALTH_REFRESH_KEYS = setOf(
            "health_metric",
            "countdown_start_date",
            "event_date",
            "day_counter_mode"
        )
    }

    override fun createBaseEngine(): BaseEngine {
        return DayCounterEngine()
    }

    inner class DayCounterEngine : BaseEngine() {

        private data class HealthLoadState(
            val renderCache: Map<LocalDate, Float>,
            val requiresFullBackfill: Boolean,
            val shouldRefreshImmediately: Boolean
        )

        private val dayCounterModule = DayCounterModule()
        private val healthCacheManager by lazy { HealthCacheManager(this@DayCounterService) }
        private val healthConnectManager by lazy { HealthConnectManager(this@DayCounterService) }
        private val healthScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private var healthRefreshJob: Job? = null
        private val cachedHealthData = mutableMapOf<LocalDate, Float>()
        private var lastHealthRefreshEpochMs: Long = 0L

        private val healthRefreshRunnable = Runnable {
            if (!isRenderingVisible) return@Runnable
            refreshHealthData()
            scheduleNextHealthRefresh()
        }

        override fun hasPreferences(): Boolean {
            return preferencesManager.hasDayCounterPreferences()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                scheduleNextHealthRefresh()
            } else {
                cancelHealthRefreshWork()
            }
        }

        override fun onDestroy() {
            cancelHealthRefreshWork()
            healthScope.cancel()
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            super.onSharedPreferenceChanged(sharedPreferences, key)
            if (!isRenderingVisible) return
            if (key in HEALTH_RELOAD_ONLY_KEYS) return
            if (key != null && key !in HEALTH_REFRESH_KEYS) return

            val preferences = runCatching { preferencesManager.getPreferences() }.getOrNull() ?: return
            if (!isHealthEnabled(preferences)) {
                cachedHealthData.clear()
                lastHealthRefreshEpochMs = 0L
                cancelHealthRefreshWork()
                return
            }

            refreshHealthData(forceFullBackfill = true, ignoreThrottle = true)
            scheduleNextHealthRefresh()
        }

        override fun initializeRendererAsync() {
            healthScope.launch {
                val preferences = runCatching { preferencesManager.getPreferences() }.getOrNull()
                if (preferences == null) {
                    withContext(Dispatchers.Main) {
                        initializeRenderer(null)
                    }
                    return@launch
                }

                val healthState = loadHealthState(preferences)
                withContext(Dispatchers.Main) {
                    initializeRenderer(healthState.renderCache)
                    if (isRenderingVisible && healthState.shouldRefreshImmediately) {
                        refreshHealthData(
                            forceFullBackfill = healthState.requiresFullBackfill,
                            ignoreThrottle = healthState.requiresFullBackfill
                        )
                    }
                }
            }
        }

        override fun initializeRenderer(healthCache: Map<LocalDate, Float>?) {
            super.initializeRenderer(healthCache)

            val preferences = runCatching { preferencesManager.getPreferences() }.getOrNull() ?: return
            if (!isHealthEnabled(preferences)) return

            renderer?.updateHealthData(
                preferences.healthMetric,
                preferences.healthMetricGoal,
                preferences.showStatOverlay,
                healthCache ?: emptyMap()
            )
        }

        override fun getGridState(preferences: UserPreferences): GridState? {
            if (!hasPreferences()) return null

            val today = LocalDate.now()
            val totalDays = dayCounterModule.calculateTotalItems(preferences)
            val pastDays = dayCounterModule.calculatePastItems(preferences, today)
            val currentIndex = dayCounterModule.calculateCurrentItemIndex(preferences, today)
            val remainingDays = maxOf(0, totalDays - pastDays - 1)
            val effectiveStartDate = dayCounterModule.getEffectiveStartDate(preferences, today)

            return GridState(
                totalItems = totalDays,
                pastItems = pastDays,
                currentIndex = currentIndex,
                futureItems = remainingDays,
                startDate = effectiveStartDate,
                expectedTotal = totalDays
            )
        }

        override fun performMidnightUpdate(preferences: UserPreferences) {
            val newState = getGridState(preferences)
            if (newState != null) {
                renderer?.updateGridState(newState)
            }

            if (isHealthEnabled(preferences)) {
                val today = LocalDate.now()
                val boundaryStart = maxDate(
                    dayCounterModule.getEffectiveStartDate(preferences, today),
                    today.minusDays(1)
                )
                refreshHealthData(
                    forceFullBackfill = false,
                    ignoreThrottle = true,
                    fetchStartOverride = boundaryStart
                )
            }

            scheduleNextHealthRefresh()
        }

        private fun loadHealthState(preferences: UserPreferences): HealthLoadState {
            if (!isHealthEnabled(preferences)) {
                cachedHealthData.clear()
                lastHealthRefreshEpochMs = 0L
                return HealthLoadState(
                    renderCache = emptyMap(),
                    requiresFullBackfill = false,
                    shouldRefreshImmediately = false
                )
            }

            val today = LocalDate.now()
            val requiredStart = dayCounterModule.getEffectiveStartDate(preferences, today)
            val snapshot = healthCacheManager.getSnapshot()
            val metricMatches = snapshot.metric == preferences.healthMetric
            val normalizedData = if (metricMatches) {
                snapshot.data
                    .filterKeys { !it.isBefore(requiredStart) && !it.isAfter(today) }
                    .toMutableMap()
            } else {
                mutableMapOf()
            }

            cachedHealthData.clear()
            cachedHealthData.putAll(normalizedData)
            lastHealthRefreshEpochMs = if (metricMatches) snapshot.lastRefreshEpochMs else 0L

            val snapshotUsable = isSnapshotUsable(snapshot, preferences, today)
            val isStale = !snapshotUsable || isHealthRefreshDue()

            return HealthLoadState(
                renderCache = normalizedData,
                requiresFullBackfill = !snapshotUsable,
                shouldRefreshImmediately = isStale
            )
        }

        private fun refreshHealthData(
            forceFullBackfill: Boolean = false,
            ignoreThrottle: Boolean = false,
            fetchStartOverride: LocalDate? = null
        ) {
            val preferences = runCatching { preferencesManager.getPreferences() }.getOrNull() ?: return
            if (!hasPreferences() || !isHealthEnabled(preferences)) return
            if (healthRefreshJob?.isActive == true) return
            if (!ignoreThrottle && !forceFullBackfill && !isHealthRefreshDue()) return

            healthRefreshJob = healthScope.launch {
                try {
                    if (!healthConnectManager.hasPermissions(preferences.healthMetric)) {
                        Log.d(TAG, "Skipping health refresh because Health Connect permission is missing")
                        return@launch
                    }
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Health Connect unavailable during permission check", e)
                    return@launch
                }

                val today = LocalDate.now()
                val requiredStart = dayCounterModule.getEffectiveStartDate(preferences, today)
                val snapshot = healthCacheManager.getSnapshot()
                val needsFullBackfill = forceFullBackfill || !isSnapshotUsable(snapshot, preferences, today)
                val fetchStart = if (needsFullBackfill) {
                    requiredStart
                } else {
                    fetchStartOverride ?: incrementalRefreshStart(preferences.healthMetric, requiredStart, today)
                }

                val normalizedData = normalizeDateWindow(
                    fetchStart,
                    today,
                    healthConnectManager.fetchAggregateData(preferences.healthMetric, fetchStart, today)
                )

                if (needsFullBackfill) {
                    cachedHealthData.clear()
                    cachedHealthData.putAll(normalizedData)
                    healthCacheManager.saveSnapshot(
                        HealthCacheSnapshot(
                            metric = preferences.healthMetric,
                            rangeStart = requiredStart,
                            rangeEnd = today,
                            lastRefreshEpochMs = System.currentTimeMillis(),
                            data = normalizedData
                        ),
                        replace = true
                    )
                } else {
                    val prunedStaleData = mergeHealthWindow(normalizedData, requiredStart, today)
                    if (prunedStaleData) {
                        healthCacheManager.saveSnapshot(
                            HealthCacheSnapshot(
                                metric = preferences.healthMetric,
                                rangeStart = requiredStart,
                                rangeEnd = today,
                                lastRefreshEpochMs = System.currentTimeMillis(),
                                data = cachedHealthData.toMap()
                            ),
                            replace = true
                        )
                    } else {
                        healthCacheManager.upsertSnapshotData(
                            metric = preferences.healthMetric,
                            rangeStart = fetchStart,
                            rangeEnd = today,
                            lastRefreshEpochMs = System.currentTimeMillis(),
                            data = normalizedData
                        )
                    }
                }

                lastHealthRefreshEpochMs = System.currentTimeMillis()
                val renderCache = cachedHealthData.toMap()

                withContext(Dispatchers.Main) {
                    val currentPreferences = runCatching { preferencesManager.getPreferences() }.getOrNull() ?: return@withContext
                    if (!isHealthEnabled(currentPreferences)) return@withContext

                    renderer?.updateHealthData(
                        currentPreferences.healthMetric,
                        currentPreferences.healthMetricGoal,
                        currentPreferences.showStatOverlay,
                        renderCache
                    )
                }
            }
        }

        private fun normalizeDateWindow(
            startDate: LocalDate,
            endDate: LocalDate,
            rawData: Map<LocalDate, Float>
        ): Map<LocalDate, Float> {
            val normalized = LinkedHashMap<LocalDate, Float>()
            var current = startDate
            while (!current.isAfter(endDate)) {
                normalized[current] = rawData[current] ?: 0f
                current = current.plusDays(1)
            }
            return normalized
        }

        private fun mergeHealthWindow(
            windowData: Map<LocalDate, Float>,
            requiredStart: LocalDate,
            today: LocalDate
        ): Boolean {
            val staleDates = cachedHealthData.keys.filter { it.isBefore(requiredStart) || it.isAfter(today) }
            staleDates.forEach(cachedHealthData::remove)
            cachedHealthData.putAll(windowData)
            return staleDates.isNotEmpty()
        }

        private fun isSnapshotUsable(
            snapshot: HealthCacheSnapshot,
            preferences: UserPreferences,
            today: LocalDate
        ): Boolean {
            if (snapshot.metric != preferences.healthMetric) return false

            val requiredStart = dayCounterModule.getEffectiveStartDate(preferences, today)
            val rangeStart = snapshot.rangeStart ?: return false
            val rangeEnd = snapshot.rangeEnd ?: return false

            return !rangeStart.isAfter(requiredStart) && !rangeEnd.isBefore(today)
        }

        private fun incrementalRefreshStart(
            metric: String,
            requiredStart: LocalDate,
            today: LocalDate
        ): LocalDate {
            val preferredStart = if (metric == HealthConnectManager.METRIC_SLEEP) {
                today.minusDays(1)
            } else {
                today
            }
            return maxDate(requiredStart, preferredStart)
        }

        private fun scheduleNextHealthRefresh() {
            handler.removeCallbacks(healthRefreshRunnable)
            if (!isRenderingVisible) return

            val preferences = runCatching { preferencesManager.getPreferences() }.getOrNull() ?: return
            if (!isHealthEnabled(preferences)) return

            handler.postDelayed(healthRefreshRunnable, HEALTH_REFRESH_INTERVAL_MS)
        }

        private fun cancelHealthRefreshWork() {
            handler.removeCallbacks(healthRefreshRunnable)
            healthRefreshJob?.cancel()
            healthRefreshJob = null
        }

        private fun isHealthEnabled(preferences: UserPreferences): Boolean {
            return preferences.healthMetric != HealthConnectManager.METRIC_NONE
        }

        private fun isHealthRefreshDue(): Boolean {
            if (lastHealthRefreshEpochMs == 0L) return true
            return System.currentTimeMillis() - lastHealthRefreshEpochMs >= HEALTH_REFRESH_INTERVAL_MS
        }

        private fun maxDate(first: LocalDate, second: LocalDate): LocalDate {
            return if (first.isAfter(second)) first else second
        }
    }
}
