package com.timehorizons.wallpaper.service

import com.timehorizons.wallpaper.data.GridState
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.modules.DayCounterModule
import java.time.LocalDate

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

        override fun performMidnightUpdate(preferences: UserPreferences) {
            // Day counter updates every day at midnight (unlike birthday logic)
            val newState = getGridState(preferences)
            if (newState != null) {
                renderer?.updateGridState(newState)
            }
        }
    }
}
