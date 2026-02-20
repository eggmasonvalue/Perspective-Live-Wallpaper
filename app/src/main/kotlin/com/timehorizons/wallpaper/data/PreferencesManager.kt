package com.timehorizons.wallpaper.data

import android.content.Context
import com.timehorizons.wallpaper.data.DayCounterMode
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Manager class for handling user preferences storage and retrieval via SharedPreferences.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "life_calendar_prefs"
        private const val KEY_BIRTH_DATE = "birth_date"
        private const val KEY_EXPECTED_LIFESPAN = "expected_lifespan"
        private const val KEY_COLOR_SCHEME_ID = "color_scheme_id"
        private const val KEY_LAST_BIRTHDAY_CHECK = "last_birthday_check"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

        // Custom color keys
        const val KEY_CUSTOM_BACKGROUND = "custom_background_color"
        const val KEY_CUSTOM_PAST_FUTURE = "custom_past_future_color"
        const val KEY_CUSTOM_CURRENT = "custom_current_color"
        const val KEY_CUSTOM_NAME = "custom_color_name"
        const val HAS_CUSTOM_COLORS = "has_custom_colors"

        // Day Counter keys
        private const val KEY_DAY_COUNTER_MODE = "day_counter_mode"
        private const val KEY_EVENT_DATE = "event_date"
        private const val KEY_EVENT_NAME = "event_name"
        private const val KEY_COUNTDOWN_START_DATE = "countdown_start_date"
        private const val KEY_DAY_COUNTER_ONBOARDING_COMPLETE = "day_counter_onboarding_complete"

        // Style keys
        private const val KEY_UNIT_SHAPE_ID = "unit_shape_id"
        private const val KEY_UNIT_SCALE = "unit_scale"
        private const val KEY_CONTAINER_PADDING_SCALE = "container_padding_scale"
    }

    /**
     * Retrieves the current UserPreferences.
     * @throws IllegalStateException if birth date is not set (should be checked via isOnboardingComplete)
     */
    fun getPreferences(): UserPreferences {
        val birthDateStr = prefs.getString(KEY_BIRTH_DATE, null)
        if (birthDateStr == null) {
            throw IllegalStateException("Birth date not set")
        }

        val birthDate = try {
            LocalDate.parse(birthDateStr)
        } catch (e: DateTimeParseException) {
            throw IllegalStateException("Invalid birth date format stored")
        }

        val expectedLifespan = prefs.getInt(KEY_EXPECTED_LIFESPAN, 90)
        val colorSchemeId = prefs.getString(KEY_COLOR_SCHEME_ID, "dark") ?: "dark"

        val lastCheckStr = prefs.getString(KEY_LAST_BIRTHDAY_CHECK, null)
        val lastCheck = if (lastCheckStr != null) {
            try { LocalDate.parse(lastCheckStr) } catch (e: Exception) { null }
        } else null

        val isOnboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

        // Day Counter fields
        val eventDateStr = prefs.getString(KEY_EVENT_DATE, null)
        val eventDate = if (eventDateStr != null) {
            try { LocalDate.parse(eventDateStr) } catch (e: Exception) { null }
        } else null

        val eventName = prefs.getString(KEY_EVENT_NAME, null)

        val startDateStr = prefs.getString(KEY_COUNTDOWN_START_DATE, null)
        val countdownStartDate = if (startDateStr != null) {
            try { LocalDate.parse(startDateStr) } catch (e: Exception) { null }
        } else null

        val isDayCounterOnboardingComplete = prefs.getBoolean(KEY_DAY_COUNTER_ONBOARDING_COMPLETE, false)

        val dayCounterMode = prefs.getString(KEY_DAY_COUNTER_MODE, DayCounterMode.STATIC) ?: DayCounterMode.STATIC
        val unitShapeId = prefs.getString(KEY_UNIT_SHAPE_ID, "rounded_square") ?: "rounded_square"
        val containerPaddingScale = prefs.getFloat(KEY_CONTAINER_PADDING_SCALE, 0.05f)
        val unitScale = prefs.getFloat(KEY_UNIT_SCALE, 1.0f)

        return UserPreferences(
            birthDate = birthDate,
            expectedLifespan = expectedLifespan,
            colorSchemeId = colorSchemeId,
            lastBirthdayCheck = lastCheck,
            isOnboardingComplete = isOnboardingComplete,
            eventDate = eventDate,
            eventName = eventName,
            countdownStartDate = countdownStartDate,
            isDayCounterOnboardingComplete = isDayCounterOnboardingComplete,
            dayCounterMode = dayCounterMode,
            unitShapeId = unitShapeId,
            unitScale = unitScale,
            containerPaddingScale = containerPaddingScale,
        )
    }

    /**
     * saves the UserPreferences to storage.
     */
    fun savePreferences(preferences: UserPreferences) {
        prefs.edit().apply {
            putString(KEY_BIRTH_DATE, preferences.birthDate.toString())
            putInt(KEY_EXPECTED_LIFESPAN, preferences.expectedLifespan)
            putString(KEY_COLOR_SCHEME_ID, preferences.colorSchemeId)
            if (preferences.lastBirthdayCheck != null) {
                putString(KEY_LAST_BIRTHDAY_CHECK, preferences.lastBirthdayCheck.toString())
            }
            putBoolean(KEY_ONBOARDING_COMPLETE, preferences.isOnboardingComplete)

            // Day Counter fields
            if (preferences.eventDate != null) {
                putString(KEY_EVENT_DATE, preferences.eventDate.toString())
            }
            if (preferences.eventName != null) {
                putString(KEY_EVENT_NAME, preferences.eventName)
            }
            if (preferences.countdownStartDate != null) {
                putString(KEY_COUNTDOWN_START_DATE, preferences.countdownStartDate.toString())
            }
            putBoolean(KEY_DAY_COUNTER_ONBOARDING_COMPLETE, preferences.isDayCounterOnboardingComplete)
            putString(KEY_DAY_COUNTER_MODE, preferences.dayCounterMode)

            putFloat(KEY_CONTAINER_PADDING_SCALE, preferences.containerPaddingScale)
            // Style fields
            putString(KEY_UNIT_SHAPE_ID, preferences.unitShapeId)
            putFloat(KEY_UNIT_SCALE, preferences.unitScale)

            apply()
        }
    }

    /**
     * Checks if the essential data (birth date) is available.
     */
    fun hasPreferences(): Boolean {
        return prefs.contains(KEY_BIRTH_DATE) && prefs.contains(KEY_EXPECTED_LIFESPAN)
    }

    /**
     * Checks if day counter preferences are available.
     */
    fun hasDayCounterPreferences(): Boolean {
        return prefs.contains(KEY_EVENT_DATE) && prefs.contains(KEY_COUNTDOWN_START_DATE)
    }

    /**
     * Updates only the last birthday check date.
     */
    fun updateLastBirthdayCheck(date: LocalDate) {
        prefs.edit().putString(KEY_LAST_BIRTHDAY_CHECK, date.toString()).apply()
    }

    /**
     * Saves a custom color scheme.
     */
    fun saveCustomColors(customColors: CustomColorScheme) {
        prefs.edit().apply {
            putString(KEY_CUSTOM_NAME, customColors.name)
            putInt(KEY_CUSTOM_BACKGROUND, customColors.backgroundColor)
            putInt(KEY_CUSTOM_PAST_FUTURE, customColors.pastFutureColor)
            putInt(KEY_CUSTOM_CURRENT, customColors.currentColor)
            putBoolean(HAS_CUSTOM_COLORS, true)
            apply()
        }
    }

    /**
     * Retrieves the custom color scheme if one exists.
     * @return CustomColorScheme if saved, null otherwise
     */
    fun getCustomColors(): CustomColorScheme? {
        if (!prefs.getBoolean(HAS_CUSTOM_COLORS, false)) {
            return null
        }

        return CustomColorScheme(
            name = prefs.getString(KEY_CUSTOM_NAME, "Custom") ?: "Custom",
            backgroundColor = prefs.getInt(KEY_CUSTOM_BACKGROUND, 0xFF000000.toInt()),
            pastFutureColor = prefs.getInt(KEY_CUSTOM_PAST_FUTURE, 0xFFFFFFFF.toInt()),
            currentColor = prefs.getInt(KEY_CUSTOM_CURRENT, 0xFFFF0000.toInt())
        )
    }

    /**
     * Checks if user has saved custom colors.
     */
    fun hasCustomColors(): Boolean {
        return prefs.getBoolean(HAS_CUSTOM_COLORS, false)
    }
}
