package com.timehorizons.wallpaper.modules

import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.utils.DateCalculator
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
        val startDate = preferences.countdownStartDate ?: LocalDate.now()
        val eventDate = preferences.eventDate ?: LocalDate.now().plusDays(30)
        val days = ChronoUnit.DAYS.between(startDate, eventDate).toInt()
        return maxOf(1, days + 1) // +1 to include both start and end dates
    }
    
    override fun calculatePastItems(preferences: UserPreferences, currentDate: LocalDate): Int {
        val startDate = preferences.countdownStartDate ?: LocalDate.now()
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
        val startDate = preferences.countdownStartDate ?: return false
        val eventDate = preferences.eventDate ?: return false
        return !eventDate.isBefore(startDate)
    }
}
