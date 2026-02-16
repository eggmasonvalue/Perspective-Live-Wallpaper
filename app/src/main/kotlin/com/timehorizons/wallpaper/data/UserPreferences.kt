package com.timehorizons.wallpaper.data

import java.time.LocalDate

/**
 * Data class representing the user's personalization settings.
 *
 * @property birthDate User's date of birth (required for calculations)
 * @property expectedLifespan Expected lifespan in years (default: 90)
 * @property colorSchemeId ID of the selected color scheme (default: "dark")
 * @property lastBirthdayCheck Date when the last birthday check was performed (for notifications/updates)
 * @property isOnboardingComplete Whether the user has completed the onboarding flow
 */
data class UserPreferences(
    val birthDate: LocalDate,
    val expectedLifespan: Int = 90,
    val colorSchemeId: String = "dark",
    val lastBirthdayCheck: LocalDate? = null,
    val isOnboardingComplete: Boolean = false,
    // Day Counter specific fields
    val eventDate: LocalDate? = null,
    val eventName: String? = null,
    val countdownStartDate: LocalDate? = null,
    val isDayCounterOnboardingComplete: Boolean = false,
    val dayCounterMode: String = "STATIC",
    // Style settings
    val unitShapeId: String = "rounded_square",
    val unitScale: Float = 1.0f
)
