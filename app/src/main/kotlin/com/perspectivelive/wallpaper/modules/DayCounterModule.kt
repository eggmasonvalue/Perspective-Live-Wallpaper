package com.perspectivelive.wallpaper.modules

import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.utils.DateCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Module implementation for counting down days to a user-specified event,
 * momentum modes, and auto-rolling current-year grids (days or 52 weeks).
 */
class DayCounterModule : CountdownModule {

    override val id: String = "day_counter"
    override val displayName: String = "Day Counter"

    override fun calculateTotalItems(preferences: UserPreferences): Int {
        return calculateTotalItems(preferences, LocalDate.now())
    }

    fun calculateTotalItems(preferences: UserPreferences, currentDate: LocalDate): Int {
        return when (preferences.dayCounterMode) {
            DayCounterMode.THIS_YEAR -> currentDate.lengthOfYear()
            DayCounterMode.THIS_YEAR_WEEKS -> TOTAL_WEEKS_IN_YEAR
            else -> {
                val startDate = getEffectiveStartDate(preferences, currentDate)
                val eventDate = getEffectiveEventDate(preferences, currentDate)
                val days = ChronoUnit.DAYS.between(startDate, eventDate).toInt()
                maxOf(1, days + 1) // +1 to include both start and end dates
            }
        }
    }

    override fun calculatePastItems(preferences: UserPreferences, currentDate: LocalDate): Int {
        return when (preferences.dayCounterMode) {
            DayCounterMode.THIS_YEAR -> maxOf(0, currentDate.dayOfYear - 1)
            DayCounterMode.THIS_YEAR_WEEKS -> {
                ((currentDate.dayOfYear - 1) / DAYS_PER_WEEK).coerceIn(0, TOTAL_WEEKS_IN_YEAR - 1)
            }
            else -> {
                val startDate = getEffectiveStartDate(preferences, currentDate)
                if (currentDate.isBefore(startDate)) {
                    0
                } else {
                    maxOf(0, ChronoUnit.DAYS.between(startDate, currentDate).toInt())
                }
            }
        }
    }

    override fun calculateCurrentItemIndex(preferences: UserPreferences, currentDate: LocalDate): Int {
        // Current item index is the same as past items (0-indexed)
        return calculatePastItems(preferences, currentDate)
    }

    override fun getNextUpdateTime(currentDate: LocalDateTime): Long {
        return DateCalculator.getNextMidnight()
    }

    companion object {
        private const val DEFAULT_FUTURE_DAYS = 30L
        const val TOTAL_WEEKS_IN_YEAR = 52
        private const val DAYS_PER_WEEK = 7
        private const val FIRST_MONTH = 1
        private const val FIRST_DAY = 1
        private const val LAST_MONTH = 12
        private const val LAST_DAY = 31
    }

    override fun validatePreferences(preferences: UserPreferences): Boolean {
        if (DayCounterMode.isDynamic(preferences.dayCounterMode)) return true

        val startDate = preferences.countdownStartDate ?: return false
        val eventDate = preferences.eventDate ?: return false
        return !eventDate.isBefore(startDate)
    }

    fun getEffectiveStartDate(
        preferences: UserPreferences,
        currentDate: LocalDate = LocalDate.now()
    ): LocalDate {
        return when (preferences.dayCounterMode) {
            DayCounterMode.NO_TOMORROW -> currentDate
            DayCounterMode.VS_YESTERDAY -> currentDate.minusDays(1)
            DayCounterMode.THIS_YEAR, DayCounterMode.THIS_YEAR_WEEKS -> {
                LocalDate.of(currentDate.year, FIRST_MONTH, FIRST_DAY)
            }
            else -> preferences.countdownStartDate ?: currentDate
        }
    }

    fun getEffectiveEventDate(
        preferences: UserPreferences,
        currentDate: LocalDate = LocalDate.now()
    ): LocalDate {
        return when (preferences.dayCounterMode) {
            DayCounterMode.NO_TOMORROW -> currentDate
            DayCounterMode.VS_YESTERDAY -> currentDate
            DayCounterMode.THIS_YEAR, DayCounterMode.THIS_YEAR_WEEKS -> {
                LocalDate.of(currentDate.year, LAST_MONTH, LAST_DAY)
            }
            else -> preferences.eventDate ?: currentDate.plusDays(DEFAULT_FUTURE_DAYS)
        }
    }
}
