package com.perspectivelive.wallpaper.service

import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.utils.DateCalculator
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
