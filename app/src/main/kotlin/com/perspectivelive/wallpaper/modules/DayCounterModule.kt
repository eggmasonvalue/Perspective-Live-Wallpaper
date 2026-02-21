package com.perspectivelive.wallpaper.modules

import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.utils.DateCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Module implementation for counting down days to a user-specified event.
 * Uses the same grid visualization as LifeCalendarModule but with days instead of years.
 */
class DayCounterModule : CountdownModule {

    override val id: String = "day_counter"
    override val displayName: String = "Day Counter"

    override fun calculateTotalItems(preferences: UserPreferences): Int {
        val startDate = getStartDate(preferences)
        val eventDate = getEventDate(preferences)
        val days = ChronoUnit.DAYS.between(startDate, eventDate).toInt()
        return maxOf(1, days + 1) // +1 to include both start and end dates
    }

    override fun calculatePastItems(preferences: UserPreferences, currentDate: LocalDate): Int {
        val startDate = getStartDate(preferences)
        if (currentDate.isBefore(startDate)) return 0
        val daysElapsed = ChronoUnit.DAYS.between(startDate, currentDate).toInt()
        return maxOf(0, daysElapsed)
    }

    override fun calculateCurrentItemIndex(preferences: UserPreferences, currentDate: LocalDate): Int {
        // Current day index is the same as past items (0-indexed)
        return calculatePastItems(preferences, currentDate)
    }

    override fun getNextUpdateTime(currentDate: LocalDateTime): Long {
        return DateCalculator.getNextMidnight()
    }

    override fun validatePreferences(preferences: UserPreferences): Boolean {
        if (preferences.dayCounterMode == DayCounterMode.NO_TOMORROW || preferences.dayCounterMode == DayCounterMode.VS_YESTERDAY) return true

        val startDate = preferences.countdownStartDate ?: return false
        val eventDate = preferences.eventDate ?: return false
        return !eventDate.isBefore(startDate)
    }

    private fun getStartDate(preferences: UserPreferences): LocalDate {
        return when (preferences.dayCounterMode) {
            DayCounterMode.NO_TOMORROW -> LocalDate.now()
            DayCounterMode.VS_YESTERDAY -> LocalDate.now().minusDays(1)
            else -> preferences.countdownStartDate ?: LocalDate.now()
        }
    }

    private fun getEventDate(preferences: UserPreferences): LocalDate {
        return when (preferences.dayCounterMode) {
            DayCounterMode.NO_TOMORROW -> LocalDate.now()
            DayCounterMode.VS_YESTERDAY -> LocalDate.now()
            else -> preferences.eventDate ?: LocalDate.now().plusDays(30)
        }
    }
}
