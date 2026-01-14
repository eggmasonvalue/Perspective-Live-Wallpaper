package com.timehorizons.wallpaper.utils

import android.graphics.Color

/**
 * Utility object for color manipulation and adaptation.
 */
object ColorUtils {
    
    /**
     * Adapts a base color for the time of day.
     * If it's nighttime (8 PM to 6 AM), dims the color by 30%.
     * 
     * @param baseColor The original ARGB color
     * @param hour The current hour of the day (0-23)
     * @return The adapted ARGB color
     */
    fun adaptBackgroundForTimeOfDay(baseColor: Int, hour: Int): Int {
        val isDaytime = hour in 6..19  // 6 AM to 8 PM (inclusive)
        
        return if (isDaytime) {
            baseColor  // Use base color as-is
        } else {
            // Darken by 30% for nighttime
            darkenColor(baseColor, 0.3f)
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
