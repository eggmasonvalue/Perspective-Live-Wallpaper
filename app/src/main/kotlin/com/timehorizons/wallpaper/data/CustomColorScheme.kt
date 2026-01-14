package com.timehorizons.wallpaper.data

/**
 * Data class for storing user's custom color selections.
 * 
 * @property name User-defined name for the custom color scheme
 * @property backgroundColor ARGB color for the background
 * @property pastFutureColor ARGB color for past and future year dots
 * @property currentColor ARGB color for the current year dot (accent)
 */
data class CustomColorScheme(
    val name: String = "Custom",
    val backgroundColor: Int,
    val pastFutureColor: Int,
    val currentColor: Int
)
