package com.timehorizons.wallpaper.modules

import com.timehorizons.wallpaper.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayCounterModuleTest {

    private val module = DayCounterModule()

    @Test
    fun testNoTomorrow_DynamicMode() {
        // In dynamic mode, dates are calculated based on LocalDate.now()
        // so we can't easily test "tomorrow" without mocking now().
        // But we can test that for "today", it behaves as expected.

        val today = LocalDate.now()

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = "NO_TOMORROW",
            eventName = "No Tomorrow"
        )

        // Logic: Start=Today, Event=Today.

        val totalItems = module.calculateTotalItems(prefs)
        // Expected: 1
        assertEquals(1, totalItems)

        val pastItems = module.calculatePastItems(prefs, today)
        // Expected: 0
        assertEquals(0, pastItems)

        // This confirms that for "today", it shows 1 item, which is current (0 past).
        // Breathing shape visible.
    }

    @Test
    fun testVsYesterday_DynamicMode() {
        val today = LocalDate.now()

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = "VS_YESTERDAY",
            eventName = "Rise Above"
        )

        // Logic: Start=Yesterday, Event=Today.
        // Total = between(Yesterday, Today) + 1 = 2.

        val totalItems = module.calculateTotalItems(prefs)
        assertEquals(2, totalItems)

        val pastItems = module.calculatePastItems(prefs, today)
        // Start=Yesterday. Current=Today.
        // Elapsed = 1 day.
        // Past = 1.
        assertEquals(1, pastItems)

        // Current index = 1.
        // Item 0 is past. Item 1 is current.
        // Breathing shape visible (item 1).
    }

    @Test
    fun testStaticMode_Standard() {
        val today = LocalDate.now()
        val future = today.plusDays(10)

        val prefs = UserPreferences(
            birthDate = LocalDate.of(1990, 1, 1),
            dayCounterMode = "STATIC",
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
