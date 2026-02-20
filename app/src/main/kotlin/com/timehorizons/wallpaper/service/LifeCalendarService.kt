package com.timehorizons.wallpaper.service

import com.timehorizons.wallpaper.data.GridState
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.utils.DateCalculator
import java.time.LocalDate

/**
 * Main wallpaper service for Life Calendar mode.
 * Extends BaseWallpaperService to reuse common logic.
 */
class LifeCalendarService : BaseWallpaperService() {

    override fun createBaseEngine(): BaseEngine {
        return LifeCalendarEngine()
    }

    inner class LifeCalendarEngine : BaseEngine() {

        override fun hasPreferences(): Boolean {
            return preferencesManager.hasPreferences()
        }

        override fun getGridState(preferences: UserPreferences): GridState? {
            if (!hasPreferences()) return null
            return GridState.calculate(preferences)
        }

        override fun performMidnightUpdate(preferences: UserPreferences) {
            val today = LocalDate.now()
            if (DateCalculator.isBirthdayToday(preferences.birthDate, today)) {
                val newGridState = GridState.calculate(preferences, today)
                renderer?.updateGridState(newGridState)
                preferencesManager.updateLastBirthdayCheck(today)
            }
        }
    }
}
