package com.perspectivelive.wallpaper.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.CustomColorScheme
import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.PreferencesManager
import com.perspectivelive.wallpaper.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * ViewModel for the Perspective - Live companion studio.
 * Exposes immutable [UserPreferences] and saved [CustomColorScheme] via [StateFlow].
 */
@Suppress("TooManyFunctions")
class SettingsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    companion object {
        private const val DEFAULT_AGE_OFFSET = 25L
        private const val DEFAULT_LIFESPAN = 90
        private const val MIN_LIFESPAN = 1
        private const val MAX_LIFESPAN = 120
        private const val DEFAULT_EVENT_OFFSET_DAYS = 30L
        private const val MIN_UNIT_SCALE = 0.5f
        private const val MAX_UNIT_SCALE = 1.0f
        private const val MIN_PADDING_SCALE = 0.0f
        private const val MAX_PADDING_SCALE = 0.2f
        private const val MIN_PULSE_MS = 500L
        private const val MAX_PULSE_MS = 5000L
    }

    private val _userPreferences = MutableStateFlow(loadInitialPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private val _customColors = MutableStateFlow(preferencesManager.getCustomColors())
    val customColors: StateFlow<CustomColorScheme?> = _customColors.asStateFlow()

    private fun loadInitialPreferences(): UserPreferences {
        if (!preferencesManager.hasPreferences()) {
            createDefaultPreferences()
        }

        val loaded = runCatching { preferencesManager.getPreferences() }
            .getOrElse {
                createDefaultPreferences()
                preferencesManager.getPreferences()
            }

        return ensureDayCounterDefaults(loaded)
    }

    private fun createDefaultPreferences() {
        val defaultDate = LocalDate.now().minusYears(DEFAULT_AGE_OFFSET)
        val defaultPrefs = UserPreferences(
            birthDate = defaultDate,
            expectedLifespan = DEFAULT_LIFESPAN,
            colorSchemeId = ColorSchemeProvider.DEFAULT_SCHEME_ID,
            isOnboardingComplete = true
        )
        preferencesManager.savePreferences(defaultPrefs)
    }

    private fun ensureDayCounterDefaults(prefs: UserPreferences): UserPreferences {
        if (prefs.eventDate != null && prefs.eventName != null && prefs.countdownStartDate != null) {
            return prefs
        }
        val today = LocalDate.now()
        val updated = prefs.copy(
            eventDate = prefs.eventDate ?: today.plusDays(DEFAULT_EVENT_OFFSET_DAYS),
            eventName = prefs.eventName ?: "My Objective",
            countdownStartDate = prefs.countdownStartDate ?: today,
            isDayCounterOnboardingComplete = true
        )
        preferencesManager.savePreferences(updated)
        return updated
    }

    private fun updateAndPersist(transform: (UserPreferences) -> UserPreferences) {
        val updated = transform(_userPreferences.value)
        _userPreferences.value = updated
        preferencesManager.savePreferences(updated)
    }

    // ===== Macro (Life Calendar) =====

    fun updateBirthDate(date: LocalDate): Boolean {
        if (!date.isBefore(LocalDate.now())) return false
        updateAndPersist { it.copy(birthDate = date) }
        return true
    }

    fun updateExpectedLifespan(lifespan: Int) {
        val clamped = lifespan.coerceIn(MIN_LIFESPAN, MAX_LIFESPAN)
        updateAndPersist { it.copy(expectedLifespan = clamped) }
    }

    // ===== Micro (Momentum / Day Counter) =====

    fun setNoTomorrowMode() {
        val today = LocalDate.now()
        updateAndPersist {
            it.copy(
                eventName = "No Tomorrow",
                countdownStartDate = today,
                eventDate = today,
                dayCounterMode = DayCounterMode.NO_TOMORROW
            )
        }
    }

    fun setVsYesterdayMode() {
        val today = LocalDate.now()
        updateAndPersist {
            it.copy(
                eventName = "Rise Above",
                countdownStartDate = today.minusDays(1),
                eventDate = today,
                dayCounterMode = DayCounterMode.VS_YESTERDAY
            )
        }
    }

    fun setCustomObjectiveMode() {
        val today = LocalDate.now()
        updateAndPersist { prefs ->
            val currentStart = prefs.countdownStartDate ?: today
            val currentTarget = prefs.eventDate
            val targetDate = if (currentTarget == null || !currentTarget.isAfter(currentStart)) {
                today.plusDays(DEFAULT_EVENT_OFFSET_DAYS)
            } else {
                currentTarget
            }
            val name = when (prefs.eventName) {
                null, "", "No Tomorrow", "Rise Above" -> "My Objective"
                else -> prefs.eventName
            }
            prefs.copy(
                eventName = name,
                countdownStartDate = currentStart,
                eventDate = targetDate,
                dayCounterMode = DayCounterMode.STATIC
            )
        }
    }

    fun updateEventName(name: String) {
        updateAndPersist { it.copy(eventName = name) }
    }

    fun updateEventDate(date: LocalDate): Boolean {
        val startDate = _userPreferences.value.countdownStartDate ?: LocalDate.now()
        if (date.isBefore(startDate)) return false
        updateAndPersist {
            it.copy(
                eventDate = date,
                countdownStartDate = startDate,
                dayCounterMode = DayCounterMode.STATIC
            )
        }
        return true
    }

    fun updateStartDate(date: LocalDate): Boolean {
        val eventDate = _userPreferences.value.eventDate ?: date.plusDays(DEFAULT_EVENT_OFFSET_DAYS)
        if (date.isAfter(eventDate)) return false
        updateAndPersist {
            it.copy(
                countdownStartDate = date,
                eventDate = eventDate,
                dayCounterMode = DayCounterMode.STATIC
            )
        }
        return true
    }

    // ===== Health Connect (Micro Exclusive) =====

    fun updateHealthMetric(metric: String, defaultGoal: Float? = null) {
        updateAndPersist {
            it.copy(
                healthMetric = metric,
                healthMetricGoal = defaultGoal ?: it.healthMetricGoal
            )
        }
    }

    fun updateHealthGoal(goal: Float) {
        if (goal <= 0f) return
        updateAndPersist { it.copy(healthMetricGoal = goal) }
    }

    fun updateShowStatOverlay(show: Boolean) {
        updateAndPersist { it.copy(showStatOverlay = show) }
    }

    // ===== Palette =====

    fun selectColorScheme(schemeId: String) {
        updateAndPersist {
            it.copy(
                colorSchemeId = schemeId,
                isDailyRotationEnabled = false
            )
        }
    }

    fun setDailyRotationEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(isDailyRotationEnabled = enabled) }
    }

    fun saveCustomColors(custom: CustomColorScheme) {
        preferencesManager.saveCustomColors(custom)
        _customColors.value = custom
        updateAndPersist {
            it.copy(
                colorSchemeId = "custom",
                isDailyRotationEnabled = false
            )
        }
    }

    // ===== Craft (Geometry & Motion) =====

    fun updateUnitShape(shapeId: String) {
        updateAndPersist { it.copy(unitShapeId = shapeId) }
    }

    fun updateUnitScale(scale: Float) {
        updateAndPersist { it.copy(unitScale = scale.coerceIn(MIN_UNIT_SCALE, MAX_UNIT_SCALE)) }
    }

    fun updateContainerPaddingScale(paddingScale: Float) {
        updateAndPersist {
            it.copy(containerPaddingScale = paddingScale.coerceIn(MIN_PADDING_SCALE, MAX_PADDING_SCALE))
        }
    }

    fun updatePulsePeriod(periodMs: Long) {
        updateAndPersist { it.copy(pulsePeriodMs = periodMs.coerceIn(MIN_PULSE_MS, MAX_PULSE_MS)) }
    }

    fun savePreferences() {
        preferencesManager.savePreferences(_userPreferences.value)
    }
}

class SettingsViewModelFactory(private val preferencesManager: PreferencesManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
