package com.perspectivelive.wallpaper.utils

import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UserPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Utility object for date and time calculations.
 */
object DateCalculator {

    /**
     * Calculates the grid state based on start date and total items.
     * Delegates to GridState's calculation logic.
     */
    fun calculateGridState(
        startDate: LocalDate,
        totalItems: Int,
        currentDate: LocalDate = LocalDate.now()
    ): GridState {
        return GridState.calculate(startDate, totalItems, currentDate)
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
