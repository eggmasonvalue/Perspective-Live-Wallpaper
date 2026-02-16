package com.timehorizons.wallpaper.settings

import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.timehorizons.wallpaper.R
import com.timehorizons.wallpaper.data.ColorSchemeProvider
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.service.DayCounterService
import com.timehorizons.wallpaper.service.LifeCalendarService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Settings screen for Time Horizons wallpaper.
 * Provides tabs to switch between Count to Life and Count to Day modes.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private var draftPreferences: UserPreferences? = null
    
    // Tab and sections
    private lateinit var modeTabLayout: TabLayout
    private lateinit var lifeCalendarSection: ScrollView
    private lateinit var dayCounterSection: ScrollView
    
    // Life Calendar views
    private lateinit var currentBirthDate: TextView
    private lateinit var currentLifespan: TextView
    private lateinit var currentColorScheme: TextView
    private lateinit var editBirthDateButton: Button
    private lateinit var editLifespanButton: Button
    private lateinit var changeColorSchemeButton: Button
    private lateinit var applyWallpaperButton: Button
    
    // Momentum views
    private lateinit var dcEventName: TextView
    private lateinit var dcEventDate: TextView
    private lateinit var dcDaysRemaining: TextView
    private lateinit var dcColorScheme: TextView
    private lateinit var noTomorrowButton: Button
    private lateinit var vsYesterdayButton: Button
    private lateinit var customObjectiveButton: Button
    private lateinit var customObjectiveLayout: View
    private lateinit var dcEditEventNameButton: Button
    private lateinit var dcEditEventDateButton: Button
    private lateinit var dcEditStartDateButton: Button
    private lateinit var dcChangeColorSchemeButton: Button
    private lateinit var applyDayCounterButton: Button
    
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        preferencesManager = PreferencesManager(this)
        
        initViews()
        setupTabs()
        
        // Check if life calendar onboarding is complete, if not create defaults
        if (!preferencesManager.hasPreferences()) {
            createDefaultPreferences()
        }
        
        // Initialize draft preferences from stored preferences
        try {
            draftPreferences = preferencesManager.getPreferences()
        } catch (e: Exception) {
            createDefaultPreferences()
        }
        
        // Initialize day counter defaults if not set
        initDayCounterDefaults()
        
        displayLifeCalendarSettings()
        displayDayCounterSettings()
        setupButtons()
    }
    
    override fun onResume() {
        super.onResume()
        if (draftPreferences == null && preferencesManager.hasPreferences()) {
            try {
                draftPreferences = preferencesManager.getPreferences()
                displayLifeCalendarSettings()
                displayDayCounterSettings()
            } catch (e: Exception) {
                // Ignore
            }
        } else if (draftPreferences != null) {
            displayDayCounterSettings()
        }
    }
    
    private fun initViews() {
        // Tabs and sections
        modeTabLayout = findViewById(R.id.modeTabLayout)
        lifeCalendarSection = findViewById(R.id.lifeCalendarSection)
        dayCounterSection = findViewById(R.id.dayCounterSection)
        
        // Life Calendar
        currentBirthDate = findViewById(R.id.currentBirthDate)
        currentLifespan = findViewById(R.id.currentLifespan)
        currentColorScheme = findViewById(R.id.currentColorScheme)
        editBirthDateButton = findViewById(R.id.editBirthDateButton)
        editLifespanButton = findViewById(R.id.editLifespanButton)
        changeColorSchemeButton = findViewById(R.id.changeColorSchemeButton)
        applyWallpaperButton = findViewById(R.id.applyWallpaperButton)
        
        // Day Counter
        dcEventName = findViewById(R.id.dcEventName)
        dcEventDate = findViewById(R.id.dcEventDate)
        dcDaysRemaining = findViewById(R.id.dcDaysRemaining)
        dcColorScheme = findViewById(R.id.dcColorScheme)
        noTomorrowButton = findViewById(R.id.noTomorrowButton)
        vsYesterdayButton = findViewById(R.id.vsYesterdayButton)
        customObjectiveButton = findViewById(R.id.customObjectiveButton)
        customObjectiveLayout = findViewById(R.id.customObjectiveLayout)
        dcEditEventNameButton = findViewById(R.id.dcEditEventNameButton)
        dcEditEventDateButton = findViewById(R.id.dcEditEventDateButton)
        dcEditStartDateButton = findViewById(R.id.dcEditStartDateButton)
        dcChangeColorSchemeButton = findViewById(R.id.dcChangeColorSchemeButton)
        applyDayCounterButton = findViewById(R.id.applyDayCounterButton)
    }
    
    private fun setupTabs() {
        // Add tabs with refined labels
        modeTabLayout.addTab(modeTabLayout.newTab().setText("Perspective"))
        modeTabLayout.addTab(modeTabLayout.newTab().setText("Momentum"))
        
        modeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        lifeCalendarSection.visibility = View.VISIBLE
                        dayCounterSection.visibility = View.GONE
                    }
                    1 -> {
                        lifeCalendarSection.visibility = View.GONE
                        dayCounterSection.visibility = View.VISIBLE
                        displayDayCounterSettings()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun createDefaultPreferences() {
        val defaultDate = LocalDate.now().minusYears(25)
        val defaultScheme = ColorSchemeProvider.getAllSchemes().firstOrNull()?.id ?: "dark"
        
        val defaultPrefs = UserPreferences(
            birthDate = defaultDate,
            expectedLifespan = 90,
            colorSchemeId = defaultScheme,
            isOnboardingComplete = true
        )
        
        preferencesManager.savePreferences(defaultPrefs)
        draftPreferences = defaultPrefs
    }
    
    /**
     * Initialize day counter with default values if not already set.
     * Default: event name "My Event", start date = today, event date = today (same as start).
     */
    private fun initDayCounterDefaults() {
        val prefs = draftPreferences ?: return
        
        if (prefs.eventDate == null || prefs.eventName == null || prefs.countdownStartDate == null) {
            val today = LocalDate.now()
            draftPreferences = prefs.copy(
                eventDate = prefs.eventDate ?: today,
                eventName = prefs.eventName ?: "My Event",
                countdownStartDate = prefs.countdownStartDate ?: today,
                isDayCounterOnboardingComplete = true
            )
            // Save defaults
            preferencesManager.savePreferences(draftPreferences!!)
        }
    }
    
    private fun displayLifeCalendarSettings() {
        val prefs = draftPreferences ?: return
        
        currentBirthDate.text = "Birth Date: ${prefs.birthDate.format(dateFormatter)}"
        currentLifespan.text = "Expected Lifespan: ${prefs.expectedLifespan} years"
        
        val schemeName = ColorSchemeProvider.getScheme(prefs.colorSchemeId, preferencesManager).name
        currentColorScheme.text = "Color Scheme: $schemeName"
    }
    
    private fun displayDayCounterSettings() {
        val prefs = draftPreferences ?: return
        
        dcEventName.text = "Event: ${prefs.eventName ?: "Not set"}"
        dcEventDate.text = "Event Date: ${prefs.eventDate?.format(dateFormatter) ?: "Not set"}"
        
        // Calculate and display days remaining from today to event date
        val eventDate = prefs.eventDate
        val startDate = prefs.countdownStartDate
        if (eventDate != null) {
            val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)
            dcDaysRemaining.text = "Days Remaining: $daysRemaining"
        } else {
            dcDaysRemaining.text = "Days Remaining: --"
        }
        
        val schemeName = ColorSchemeProvider.getScheme(prefs.colorSchemeId, preferencesManager).name
        dcColorScheme.text = "Color Scheme: $schemeName"
    }
    
    private fun setupButtons() {
        // Life Calendar buttons
        editBirthDateButton.setOnClickListener { showDatePickerDialog() }
        editLifespanButton.setOnClickListener { showLifespanDialog() }
        changeColorSchemeButton.setOnClickListener { showColorSchemeDialog() }
        applyWallpaperButton.setOnClickListener { saveAndLaunchLifeCalendarPicker() }
        
        // Momentum buttons
        noTomorrowButton.setOnClickListener { setNoTomorrowMode() }
        vsYesterdayButton.setOnClickListener { setVsYesterdayMode() }
        customObjectiveButton.setOnClickListener { toggleCustomObjective() }
        dcEditEventNameButton.setOnClickListener { showEventNameDialog() }
        dcEditEventDateButton.setOnClickListener { showEventDatePickerDialog() }
        dcEditStartDateButton.setOnClickListener { showStartDatePickerDialog() }
        dcChangeColorSchemeButton.setOnClickListener { showDayCounterColorSchemeDialog() }
        applyDayCounterButton.setOnClickListener { saveAndLaunchDayCounterPicker() }
    }
    
    // ===== Life Calendar Methods =====
    
    private fun showDatePickerDialog() {
        val prefs = draftPreferences ?: return
        val currentDate = prefs.birthDate
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newBirthDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (newBirthDate.isBefore(LocalDate.now())) {
                    draftPreferences = prefs.copy(birthDate = newBirthDate)
                    displayLifeCalendarSettings()
                } else {
                    Toast.makeText(this, R.string.birth_date_validation_error, Toast.LENGTH_SHORT).show()
                }
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }
    
    private fun showLifespanDialog() {
        val prefs = draftPreferences ?: return
        
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_lifespan_picker, null)
        
        val numberPicker = view.findViewById<NumberPicker>(R.id.lifespanNumberPicker)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        
        numberPicker.minValue = 1
        numberPicker.maxValue = 120
        numberPicker.value = prefs.expectedLifespan
        numberPicker.wrapSelectorWheel = false
        
        saveButton.setOnClickListener {
            draftPreferences = prefs.copy(expectedLifespan = numberPicker.value)
            displayLifeCalendarSettings()
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun showColorSchemeDialog() {
        val prefs = draftPreferences ?: return
        
        val bottomSheet = StyleSelectionBottomSheet.newInstance(
            prefs.colorSchemeId,
            prefs.unitShapeId,
            prefs.unitScale
        )
        bottomSheet.setOnStyleAppliedListener { scheme, shapeId, scale ->
            draftPreferences = prefs.copy(
                colorSchemeId = scheme.id,
                unitShapeId = shapeId,
                unitScale = scale
            )
            displayLifeCalendarSettings()
        }
        bottomSheet.show(supportFragmentManager, "StyleSelectionBottomSheet")
    }
    
    private fun saveAndLaunchLifeCalendarPicker() {
        draftPreferences?.let { preferencesManager.savePreferences(it) }
        
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, LifeCalendarService::class.java)
            )
        }
        startActivity(intent)
    }
    
    // ===== Momentum Methods =====
    
    private fun setNoTomorrowMode() {
        val prefs = draftPreferences ?: return
        val today = LocalDate.now()
        draftPreferences = prefs.copy(
            eventName = "No Tomorrow",
            countdownStartDate = today,
            eventDate = today,
            dayCounterMode = "NO_TOMORROW"
        )
        customObjectiveLayout.visibility = View.GONE
        displayDayCounterSettings()
    }
    
    private fun setVsYesterdayMode() {
        val prefs = draftPreferences ?: return
        val today = LocalDate.now()
        draftPreferences = prefs.copy(
            eventName = "Rise Above",
            countdownStartDate = today.minusDays(1),
            eventDate = today,
            dayCounterMode = "VS_YESTERDAY"
        )
        customObjectiveLayout.visibility = View.GONE
        displayDayCounterSettings()
    }
    
    private fun toggleCustomObjective() {
        customObjectiveLayout.visibility = if (customObjectiveLayout.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
    
    private fun showEventNameDialog() {
        val prefs = draftPreferences ?: return
        
        val editText = EditText(this)
        editText.setText(prefs.eventName ?: "")
        editText.hint = getString(R.string.event_name_hint)
        
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.event_name_label)
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    draftPreferences = prefs.copy(eventName = newName)
                    displayDayCounterSettings()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEventDatePickerDialog() {
        val prefs = draftPreferences ?: return
        val currentDate = prefs.eventDate ?: LocalDate.now()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                // Update both event date and start date to be the same initially
                // Start date stays as it was, event date is the target
                draftPreferences = prefs.copy(
                    eventDate = newDate,
                    countdownStartDate = prefs.countdownStartDate ?: LocalDate.now(),
                    dayCounterMode = "STATIC"
                )
                displayDayCounterSettings()
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }
    
    private fun showStartDatePickerDialog() {
        val prefs = draftPreferences ?: return
        val currentDate = prefs.countdownStartDate ?: LocalDate.now()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                draftPreferences = prefs.copy(countdownStartDate = newDate, dayCounterMode = "STATIC")
                displayDayCounterSettings()
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }
    
    private fun showDayCounterColorSchemeDialog() {
        val prefs = draftPreferences ?: return
        
        val bottomSheet = StyleSelectionBottomSheet.newInstance(
            prefs.colorSchemeId,
            prefs.unitShapeId,
            prefs.unitScale
        )
        bottomSheet.setOnStyleAppliedListener { scheme, shapeId, scale ->
            draftPreferences = prefs.copy(
                colorSchemeId = scheme.id,
                unitShapeId = shapeId,
                unitScale = scale
            )
            displayDayCounterSettings()
        }
        bottomSheet.show(supportFragmentManager, "StyleSelectionBottomSheet")
    }
    
    private fun saveAndLaunchDayCounterPicker() {
        draftPreferences?.let { preferencesManager.savePreferences(it) }
        
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, DayCounterService::class.java)
            )
        }
        startActivity(intent)
    }
}
