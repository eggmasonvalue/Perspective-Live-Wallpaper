package com.perspectivelive.wallpaper.modules

import com.perspectivelive.wallpaper.data.UserPreferences
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Interface defining the contract for a countdown module.
 * Different modules can implement different ways of counting (e.g., Years, Sundays).
 */
interface CountdownModule {
    
    /** Unique identifier for this module */
    val id: String
    
    /** Display name for settings UI */
    val displayName: String
    
    /** Calculate how many total items to display */
    fun calculateTotalItems(preferences: UserPreferences): Int
    
    /** Calculate how many items are "past" (filled) */
    fun calculatePastItems(preferences: UserPreferences, currentDate: LocalDate): Int
    
    /** Calculate the current item index (0-based) that should pulse */
    fun calculateCurrentItemIndex(preferences: UserPreferences, currentDate: LocalDate): Int
    
    /** Determines when this module should update (e.g., daily at midnight, weekly on Sunday) */
    fun getNextUpdateTime(currentDate: LocalDateTime): Long
    
    /** Optional: Custom validation for module-specific preferences */
    fun validatePreferences(preferences: UserPreferences): Boolean = true
}
