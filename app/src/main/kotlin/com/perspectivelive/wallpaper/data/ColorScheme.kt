package com.perspectivelive.wallpaper.data

/**
 * Data class defining a visual color scheme for the wallpaper.
 *
 * @property id Unique identifier for the scheme
 * @property name Display name of the scheme
 * @property backgroundColor ARGB color for the background
 * @property pastYearsColor ARGB color for dots representing past years (filled)
 * @property currentYearColor ARGB color for the current year dot (pulsing)
 * @property futureYearsColor ARGB color for future years (dimmed/empty)
 * @property isDynamic Whether this scheme adapts to time of day (day/night mode)
 * @property isCustom Whether this is a user-created custom color scheme
 */
data class ColorScheme(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val pastYearsColor: Int,
    val currentYearColor: Int,
    val futureYearsColor: Int,
    val isDynamic: Boolean = false,
    val isCustom: Boolean = false
)
