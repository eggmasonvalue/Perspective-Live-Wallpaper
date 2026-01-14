package com.timehorizons.wallpaper.utils

import com.timehorizons.wallpaper.data.LifeState
import com.timehorizons.wallpaper.data.UserPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Utility object for date and time calculations.
 */
object DateCalculator {
    
    /**
     * Calculates the user's life state based on birth date and expected lifespan.
     * Delegates to LifeState's calculation logic.
     */
    fun calculateLifeState(
        birthDate: LocalDate,
        expectedLifespan: Int,
        currentDate: LocalDate = LocalDate.now()
    ): LifeState {



        return LifeState.calculate(birthDate, expectedLifespan, currentDate)
    }
    
    /**
     * Checks if today is the user's birthday.
     */
    fun isBirthdayToday(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        return birthDate.month == today.month && birthDate.dayOfMonth == today.dayOfMonth
    }
    
    /**
     * Calculates the timestamp (milliseconds) of the next midnight.
     * Used for scheduling updates.
     */
    fun getNextMidnight(): Long {
        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
        val zonedTomorrow = tomorrow.atZone(ZoneId.systemDefault())
        return zonedTomorrow.toInstant().toEpochMilli()
    }
}
