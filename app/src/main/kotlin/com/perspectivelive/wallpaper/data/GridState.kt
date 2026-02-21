package com.perspectivelive.wallpaper.data

import com.perspectivelive.wallpaper.modules.ModuleRegistry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Data class representing the current state of the grid to be rendered.
 * Generalized to support both life calendar (years) and day counter (days).
 *
 * @property totalItems Total number of dots to render
 * @property pastItems Number of filled dots (past)
 * @property currentIndex 0-based index of the currently pulsing dot
 * @property futureItems Number of dimmed/empty dots (future)
 * @property startDate Starting date (e.g., birth date or countdown start)
 * @property expectedTotal Expected total number of items
 */
data class GridState(
    val totalItems: Int,
    val pastItems: Int,
    val currentIndex: Int,
    val futureItems: Int,
    val startDate: LocalDate,
    val expectedTotal: Int
) {
    companion object {
        /**
         * Calculates the grid state using the active module from preferences.
         */
        fun calculate(
            preferences: UserPreferences,
            currentDate: LocalDate = LocalDate.now()
        ): GridState {
            val module = ModuleRegistry.getActiveModule(preferences)

            val totalItems = module.calculateTotalItems(preferences)
            val pastItems = module.calculatePastItems(preferences, currentDate)
            val currentIndex = module.calculateCurrentItemIndex(preferences, currentDate)
            val remainingItems = maxOf(0, totalItems - pastItems - 1)

            return GridState(
                totalItems = totalItems,
                pastItems = pastItems,
                currentIndex = currentIndex,
                futureItems = remainingItems,
                startDate = preferences.birthDate,
                expectedTotal = preferences.expectedLifespan
            )
        }

        /**
         * Legacy calculation method for backward compatibility or simple year-based logic.
         */
        fun calculate(startDate: LocalDate, totalItems: Int, today: LocalDate): GridState {
            val itemsSinceStart = ChronoUnit.YEARS.between(startDate, today).toInt()
            val pastItems = maxOf(0, itemsSinceStart)
            val currentIndex = pastItems
            val futureItems = maxOf(0, totalItems - pastItems - 1)

            return GridState(
                totalItems = totalItems,
                pastItems = pastItems,
                currentIndex = currentIndex,
                futureItems = futureItems,
                startDate = startDate,
                expectedTotal = totalItems
            )
        }
    }
}
