package com.timehorizons.wallpaper.data

/**
 * Singleton provider for the standard color schemes.
 */
object ColorSchemeProvider {
    
    private val schemes = listOf(
        // Light Themes (5)
        ColorScheme(
            id = "nordic_minimal",
            name = "Nordic Minimal",
            backgroundColor = 0xFFF8F9FA.toInt(),      // Off-white with warmth
            pastYearsColor = 0xFF7D9D9C.toInt(),       // Dusty sage green
            currentYearColor = 0xFFE76F51.toInt(),     // Terracotta accent
            futureYearsColor = 0x4D7D9D9C.toInt(),     // 30% opacity sage
            isDynamic = true
        ),
        ColorScheme(
            id = "warm_sand",
            name = "Warm Sand",
            backgroundColor = 0xFFFAF7F2.toInt(),      // Warm off-white
            pastYearsColor = 0xFFC9ADA7.toInt(),       // Dusty rose-brown
            currentYearColor = 0xFF9A8873.toInt(),     // Deep taupe
            futureYearsColor = 0x4DC9ADA7.toInt(),     // 30% opacity dusty rose
            isDynamic = true
        ),
        ColorScheme(
            id = "glacial_peak",
            name = "Glacial Peak",
            backgroundColor = 0xFFF5F8FA.toInt(),      // Cool crisp white
            pastYearsColor = 0xFF7D94B0.toInt(),       // Steel blue
            currentYearColor = 0xFFE57373.toInt(),     // Soft red accent
            futureYearsColor = 0x4D7D94B0.toInt(),
            isDynamic = true
        ),
        ColorScheme(
            id = "sage_garden",
            name = "Sage Garden",
            backgroundColor = 0xFFF2F5F2.toInt(),      // Very pale green-white
            pastYearsColor = 0xFF88A096.toInt(),       // Muted sage
            currentYearColor = 0xFFE6B8A2.toInt(),     // Soft peach
            futureYearsColor = 0x4D88A096.toInt(),
            isDynamic = true
        ),
        ColorScheme(
            id = "rose_quartz",
            name = "Rose Quartz",
            backgroundColor = 0xFFFFF9FA.toInt(),      // Softest pink-white
            pastYearsColor = 0xFFC5A3AB.toInt(),       // Dusty rose
            currentYearColor = 0xFF6D9DC5.toInt(),     // Muted serenity blue
            futureYearsColor = 0x4DC5A3AB.toInt(),
            isDynamic = true
        ),

        // Dark Themes (5)
        ColorScheme(
            id = "midnight_ocean",
            name = "Midnight Ocean",
            backgroundColor = 0xFF0B1120.toInt(),      // Deep midnight blue
            pastYearsColor = 0xFF5B9AA0.toInt(),       // Soft teal
            currentYearColor = 0xFFFFA07A.toInt(),     // Warm coral accent
            futureYearsColor = 0x4D5B9AA0.toInt(),     // 30% opacity teal
            isDynamic = true
        ),
        ColorScheme(
            id = "forest_dream",
            name = "Forest Dream",
            backgroundColor = 0xFF1A2421.toInt(),      // Deep forest green
            pastYearsColor = 0xFF82B894.toInt(),       // Soft mint green
            currentYearColor = 0xFFF4A259.toInt(),     // Golden amber
            futureYearsColor = 0x4D82B894.toInt(),     // 30% opacity mint
            isDynamic = true
        ),
        ColorScheme(
            id = "lavender_dusk",
            name = "Lavender Dusk",
            backgroundColor = 0xFF1B1725.toInt(),      // Deep purple-black
            pastYearsColor = 0xFFA78BAA.toInt(),       // Soft lavender
            currentYearColor = 0xFFF2C14E.toInt(),     // Warm gold
            futureYearsColor = 0x4DA78BAA.toInt(),     // 30% opacity lavender
            isDynamic = true
        ),
        ColorScheme(
            id = "monochrome_zen",
            name = "Monochrome Zen",
            backgroundColor = 0xFF121212.toInt(),      // True dark
            pastYearsColor = 0xFFE8E8E8.toInt(),       // Light gray
            currentYearColor = 0xFFFFFFFF.toInt(),     // Pure white accent
            futureYearsColor = 0x4DE8E8E8.toInt(),     // 30% opacity gray
            isDynamic = true
        ),
        ColorScheme(
            id = "ember_night",
            name = "Ember Night",
            backgroundColor = 0xFF140A0A.toInt(),      // Deep warm black
            pastYearsColor = 0xFF8E5555.toInt(),       // Muted brick red
            currentYearColor = 0xFFFFB347.toInt(),     // Pastel orange
            futureYearsColor = 0x4D8E5555.toInt(),
            isDynamic = true
        )
    )
    
    /**
     * Retrieves a scheme by its ID. Defaults to the first scheme if not found.
     * Supports "custom" ID if user has saved custom colors.
     */
    fun getScheme(id: String, prefsManager: PreferencesManager? = null): ColorScheme {
        // If requesting custom scheme and we have a preferences manager
        if (id == "custom" && prefsManager != null) {
            val customColors = prefsManager.getCustomColors()
            if (customColors != null) {
                return createCustomColorScheme(customColors)
            }
        }
        
        return schemes.find { it.id == id } ?: schemes.first()
    }
    
    /**
     * Returns all available schemes.
     */
    fun getAllSchemes(): List<ColorScheme> {
        return schemes
    }
    
    /**
     * Creates a ColorScheme from a CustomColorScheme.
     * Future years color is automatically set to 30% opacity of pastFutureColor.
     */
    fun createCustomColorScheme(custom: CustomColorScheme): ColorScheme {
        return ColorScheme(
            id = "custom",
            name = custom.name,  // Use user-provided name
            backgroundColor = custom.backgroundColor,
            pastYearsColor = custom.pastFutureColor,
            currentYearColor = custom.currentColor,
            futureYearsColor = (custom.pastFutureColor and 0x00FFFFFF) or 0x4D000000, // 30% opacity
            isDynamic = false
        )
    }
}
