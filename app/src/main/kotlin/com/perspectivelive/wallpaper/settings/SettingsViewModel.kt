package com.perspectivelive.wallpaper.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.PreferencesManager
import com.perspectivelive.wallpaper.data.UserPreferences
import java.time.LocalDate

/**
 * ViewModel for managing user preferences and UI state in the Settings screen.
 * Handles loading, updating, and saving preferences, ensuring separation of concerns.
 */
class SettingsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _userPreferences = MutableLiveData<UserPreferences>()
    val userPreferences: LiveData<UserPreferences> = _userPreferences

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        if (!preferencesManager.hasPreferences()) {
            createDefaultPreferences()
        }

        try {
            _userPreferences.value = preferencesManager.getPreferences()
            initDayCounterDefaults()
        } catch (e: Exception) {
            createDefaultPreferences()
            _userPreferences.value = preferencesManager.getPreferences()
        }
    }

    private fun createDefaultPreferences() {
        val defaultDate = LocalDate.now().minusYears(25)
        val defaultScheme = "sage_garden"

        val defaultPrefs = UserPreferences(
            birthDate = defaultDate,
            expectedLifespan = 90,
            colorSchemeId = defaultScheme,
            isOnboardingComplete = true
        )

        preferencesManager.savePreferences(defaultPrefs)
    }

    private fun initDayCounterDefaults() {
        val currentPrefs = _userPreferences.value ?: return

        if (currentPrefs.eventDate == null || currentPrefs.eventName == null || currentPrefs.countdownStartDate == null) {
            val today = LocalDate.now()
            val updatedPrefs = currentPrefs.copy(
                eventDate = currentPrefs.eventDate ?: today,
                eventName = currentPrefs.eventName ?: "My Event",
                countdownStartDate = currentPrefs.countdownStartDate ?: today,
                isDayCounterOnboardingComplete = true
            )
            preferencesManager.savePreferences(updatedPrefs)
            _userPreferences.value = updatedPrefs
        }
    }

    /**
     * Updates the birth date. Returns true if valid and updated, false otherwise.
     */
    fun updateBirthDate(date: LocalDate): Boolean {
        if (!date.isBefore(LocalDate.now())) return false

        val current = _userPreferences.value ?: return false
        _userPreferences.value = current.copy(birthDate = date)
        return true
    }

    fun updateExpectedLifespan(lifespan: Int) {
        val current = _userPreferences.value ?: return
        _userPreferences.value = current.copy(expectedLifespan = lifespan)
    }

    fun updateColorScheme(schemeId: String, shapeId: String, scale: Float, paddingScale: Float = 0.05f, pulsePeriodMs: Long = 2000L) {
        val current = _userPreferences.value ?: return
        _userPreferences.value = current.copy(
            colorSchemeId = schemeId,
            unitShapeId = shapeId,
            unitScale = scale,
            containerPaddingScale = paddingScale,
            pulsePeriodMs = pulsePeriodMs
        )
    }

    // Momentum Updates
    fun setNoTomorrowMode() {
        val current = _userPreferences.value ?: return
        val today = LocalDate.now()
        _userPreferences.value = current.copy(
            eventName = "No Tomorrow",
            countdownStartDate = today,
            eventDate = today,
            dayCounterMode = DayCounterMode.NO_TOMORROW
        )
    }

    fun setVsYesterdayMode() {
        val current = _userPreferences.value ?: return
        val today = LocalDate.now()
        _userPreferences.value = current.copy(
            eventName = "Rise Above",
            countdownStartDate = today.minusDays(1),
            eventDate = today,
            dayCounterMode = DayCounterMode.VS_YESTERDAY
        )
    }

    fun updateEventName(name: String) {
        val current = _userPreferences.value ?: return
        _userPreferences.value = current.copy(eventName = name)
    }

    /**
     * Updates the event date. Returns true if valid and updated, false otherwise.
     */
    fun updateEventDate(date: LocalDate): Boolean {
        val current = _userPreferences.value ?: return false
        val startDate = current.countdownStartDate ?: LocalDate.now()

        // Ensure event date is not before start date (allow same day)
        if (date.isBefore(startDate)) return false

        _userPreferences.value = current.copy(
            eventDate = date,
            countdownStartDate = startDate,
            dayCounterMode = DayCounterMode.STATIC
        )
        return true
    }

    /**
     * Updates the start date. Returns true if valid and updated, false otherwise.
     */
    fun updateStartDate(date: LocalDate): Boolean {
        val current = _userPreferences.value ?: return false
        val eventDate = current.eventDate ?: date.plusDays(30)

        // Ensure start date is not after event date
        if (date.isAfter(eventDate)) return false

        _userPreferences.value = current.copy(
            countdownStartDate = date,
            dayCounterMode = DayCounterMode.STATIC
        )
        return true
    }

    fun savePreferences() {
        _userPreferences.value?.let { preferencesManager.savePreferences(it) }
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
