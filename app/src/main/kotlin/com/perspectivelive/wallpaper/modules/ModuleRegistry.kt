package com.perspectivelive.wallpaper.modules

import com.perspectivelive.wallpaper.data.UserPreferences

/**
 * Registry singleton to manage and access available countdown modules.
 */
object ModuleRegistry {
    
    private val modules: Map<String, CountdownModule> = mapOf(
        "life_calendar" to LifeCalendarModule(),
        "day_counter" to DayCounterModule()
    )
    
    /**
     * Retrieves a module by its ID. Defaults to "life_calendar" if not found.
     */
    fun getModule(moduleId: String): CountdownModule {
        return modules[moduleId] ?: modules["life_calendar"]!!
    }
    
    /**
     * Returns all registered modules.
     */
    fun getAllModules(): List<CountdownModule> {
        return modules.values.toList()
    }
    
    /**
     * Determines the active module based on user preferences.
     * Currently defaults to LifeCalendarModule, but ready for expansion.
     */
    fun getActiveModule(@Suppress("UNUSED_PARAMETER") preferences: UserPreferences): CountdownModule {
        // In Phase 1, we only have one module.
        // In Phase 2, we could check preferences.activeModuleId
        return getModule("life_calendar")
    }
}
