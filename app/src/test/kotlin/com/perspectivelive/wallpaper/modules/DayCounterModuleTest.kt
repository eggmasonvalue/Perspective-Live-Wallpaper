package com.perspectivelive.wallpaper.modules

import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayCounterModuleTest {

    private val module = DayCounterModule()

    @Test
    fun testNoTomorrow_DynamicMode() {
        val today = LocalDate.now()

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.NO_TOMORROW,
            eventName = "No Tomorrow"
        )

        val totalItems = module.calculateTotalItems(prefs)
        assertEquals(1, totalItems)

        val pastItems = module.calculatePastItems(prefs, today)
        assertEquals(0, pastItems)
    }

    @Test
    fun testVsYesterday_DynamicMode() {
        val today = LocalDate.now()

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.VS_YESTERDAY,
            eventName = "Rise Above"
        )

        val totalItems = module.calculateTotalItems(prefs)
        assertEquals(2, totalItems)

        val pastItems = module.calculatePastItems(prefs, today)
        assertEquals(1, pastItems)
    }

    @Test
    fun testEffectiveStartDate_NoTomorrowUsesCurrentDate() {
        val currentDate = LocalDate.of(2026, 4, 20)
        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.NO_TOMORROW
        )

        assertEquals(currentDate, module.getEffectiveStartDate(prefs, currentDate))
        assertEquals(currentDate, module.getEffectiveEventDate(prefs, currentDate))
    }

    @Test
    fun testEffectiveStartDate_VsYesterdayTracksCurrentDate() {
        val currentDate = LocalDate.of(2026, 4, 20)
        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.VS_YESTERDAY
        )

        assertEquals(currentDate.minusDays(1), module.getEffectiveStartDate(prefs, currentDate))
        assertEquals(currentDate, module.getEffectiveEventDate(prefs, currentDate))
    }

    @Test
    fun testThisYearDays_RegularAndLeapYears() {
        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.THIS_YEAR
        )

        assertTrue(module.validatePreferences(prefs))

        val jan1 = LocalDate.of(2026, 1, 1)
        assertEquals(365, module.calculateTotalItems(prefs, jan1))
        assertEquals(0, module.calculatePastItems(prefs, jan1))
        assertEquals(0, module.calculateCurrentItemIndex(prefs, jan1))
        assertEquals(LocalDate.of(2026, 1, 1), module.getEffectiveStartDate(prefs, jan1))
        assertEquals(LocalDate.of(2026, 12, 31), module.getEffectiveEventDate(prefs, jan1))

        val dec31 = LocalDate.of(2026, 12, 31)
        assertEquals(364, module.calculatePastItems(prefs, dec31))

        val leapDec31 = LocalDate.of(2024, 12, 31)
        assertEquals(366, module.calculateTotalItems(prefs, leapDec31))
        assertEquals(365, module.calculatePastItems(prefs, leapDec31))
        assertEquals(LocalDate.of(2024, 1, 1), module.getEffectiveStartDate(prefs, leapDec31))
        assertEquals(LocalDate.of(2024, 12, 31), module.getEffectiveEventDate(prefs, leapDec31))
    }

    @Test
    fun testThisYearWeeks_BoundariesAndRollover() {
        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.THIS_YEAR_WEEKS
        )

        assertTrue(module.validatePreferences(prefs))

        val jan1 = LocalDate.of(2026, 1, 1)
        assertEquals(52, module.calculateTotalItems(prefs, jan1))
        assertEquals(0, module.calculatePastItems(prefs, jan1))

        val jan7 = LocalDate.of(2026, 1, 7)
        assertEquals(0, module.calculatePastItems(prefs, jan7))

        val jan8 = LocalDate.of(2026, 1, 8)
        assertEquals(1, module.calculatePastItems(prefs, jan8))

        val dec31Regular = LocalDate.of(2026, 12, 31)
        assertEquals(51, module.calculatePastItems(prefs, dec31Regular))
        assertEquals(51, module.calculateCurrentItemIndex(prefs, dec31Regular))

        val dec31Leap = LocalDate.of(2024, 12, 31)
        assertEquals(52, module.calculateTotalItems(prefs, dec31Leap))
        assertEquals(51, module.calculatePastItems(prefs, dec31Leap))

        val nextYearJan1 = LocalDate.of(2027, 1, 1)
        assertEquals(0, module.calculatePastItems(prefs, nextYearJan1))
        assertEquals(LocalDate.of(2027, 1, 1), module.getEffectiveStartDate(prefs, nextYearJan1))
    }

    @Test
    fun testStaticMode_Standard() {
        val today = LocalDate.now()
        val future = today.plusDays(10)

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = DayCounterMode.STATIC,
            countdownStartDate = today,
            eventDate = future,
            eventName = "Static Event"
        )

        val totalItems = module.calculateTotalItems(prefs)
        assertEquals(11, totalItems)

        val pastItems = module.calculatePastItems(prefs, today)
        assertEquals(0, pastItems)
    }
}
