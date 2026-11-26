package com.perspectivelive.wallpaper.utils

import android.graphics.Color

/**
 * Utility object for color manipulation and adaptation.
 */
object ColorUtils {
    private const val DAYTIME_START_HOUR = 6
    private const val DAYTIME_END_HOUR = 19
    private const val NIGHT_DIM_FACTOR = 0.3f

    /**
     * Adapts a base color for the time of day.
     * If it's nighttime (8 PM to 6 AM), dims the color by 30%.
     *
     * @param baseColor The original ARGB color
     * @param hour The current hour of the day (0-23)
     * @return The adapted ARGB color
     */
    fun adaptBackgroundForTimeOfDay(baseColor: Int, hour: Int): Int {
        val isDaytime = hour in DAYTIME_START_HOUR..DAYTIME_END_HOUR

        return if (isDaytime) {
            baseColor // Use base color as-is
        } else {
            // Darken by 30% for nighttime
            darkenColor(baseColor, NIGHT_DIM_FACTOR)
        }
    }
    
    /**
     * Darkens a color by a specific factor.
     * 
     * @param color The ARGB color to darken
     * @param factor The factor to darken by (0.0 to 1.0, where 1.0 is black)
     * @return The darkened ARGB color
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val alpha = Color.alpha(color)
        val red = (Color.red(color) * (1 - factor)).toInt()
        val green = (Color.green(color) * (1 - factor)).toInt()
        val blue = (Color.blue(color) * (1 - factor)).toInt()
        
        return Color.argb(alpha, red, green, blue)
    }
}
