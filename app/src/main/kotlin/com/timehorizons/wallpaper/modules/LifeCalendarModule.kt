package com.timehorizons.wallpaper.modules

import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.utils.DateCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Module implementation for the standard "Life in Years" countdown.
 */
class LifeCalendarModule : CountdownModule {
    
    override val id: String = "life_calendar"
    override val displayName: String = "Life in Years"
    
    override fun calculateTotalItems(preferences: UserPreferences): Int {
        return preferences.expectedLifespan
    }
    
    override fun calculatePastItems(preferences: UserPreferences, currentDate: LocalDate): Int {
        val yearsLived = ChronoUnit.YEARS.between(preferences.birthDate, currentDate).toInt()
        // Ensure we don't return negative values if birthDate is in future (though validated elsewhere)
        return maxOf(0, yearsLived)
    }
    
    override fun calculateCurrentItemIndex(preferences: UserPreferences, currentDate: LocalDate): Int {
        // For years, the current index is the same as past items (0-indexed)
        // e.g. if lived 25 years, dot 25 (the 26th dot) is the current one
        return calculatePastItems(preferences, currentDate)
    }
    
    override fun getNextUpdateTime(currentDate: LocalDateTime): Long {
        return DateCalculator.getNextMidnight()
    }
    
    override fun validatePreferences(preferences: UserPreferences): Boolean {
        return preferences.expectedLifespan > 0 && 
               !preferences.birthDate.isAfter(LocalDate.now())
    }
    
    private fun maxOf(a: Int, b: Int): Int {
        return kotlin.math.max(a, b)
    }
}
