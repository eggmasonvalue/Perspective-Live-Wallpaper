package com.timehorizons.wallpaper.data

import com.timehorizons.wallpaper.modules.ModuleRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Data class representing the current state of the user's life timeline.
 * 
 * @property totalYears Total number of dots to render
 * @property yearsLived Number of filled dots (past)
 * @property currentYearIndex 0-based index of the currently pulsing dot
 * @property yearsRemaining Number of dimmed/empty dots (future)
 * @property birthDate User's birth date
 * @property expectedLifespan Expected total lifespan
 */
data class LifeState(
    val totalYears: Int,
    val yearsLived: Int,
    val currentYearIndex: Int,
    val yearsRemaining: Int,
    val birthDate: LocalDate,
    val expectedLifespan: Int
) {
    companion object {
        /**
         * Calculates the life state using the active module from preferences.
         */
        fun calculate(
            preferences: UserPreferences,
            currentDate: LocalDate = LocalDate.now()
        ): LifeState {
            val module = ModuleRegistry.getActiveModule(preferences)
            
            val totalItems = module.calculateTotalItems(preferences)
            val pastItems = module.calculatePastItems(preferences, currentDate)
            val currentIndex = module.calculateCurrentItemIndex(preferences, currentDate)
            val remainingItems = maxOf(0, totalItems - pastItems - 1)
            
            return LifeState(
                totalYears = totalItems,
                yearsLived = pastItems,
                currentYearIndex = currentIndex,
                yearsRemaining = remainingItems,
                birthDate = preferences.birthDate,
                expectedLifespan = preferences.expectedLifespan
            )
        }
        
        /**
         * Legacy calculation method for backward compatibility or simple year-based logic.
         */
        fun calculate(birthDate: LocalDate, expectedLifespan: Int, today: LocalDate): LifeState {
            val yearsSinceBirth = ChronoUnit.YEARS.between(birthDate, today).toInt()
            val yearsLived = maxOf(0, yearsSinceBirth)
            val currentYearIndex = yearsLived
            val yearsRemaining = maxOf(0, expectedLifespan - yearsLived - 1)
            
            return LifeState(
                totalYears = expectedLifespan,
                yearsLived = yearsLived,
                currentYearIndex = currentYearIndex,
                yearsRemaining = yearsRemaining,
                birthDate = birthDate,
                expectedLifespan = expectedLifespan
            )
        }
    }
}
