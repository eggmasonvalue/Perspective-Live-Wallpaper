package com.timehorizons.wallpaper.settings

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timehorizons.wallpaper.R
import com.timehorizons.wallpaper.data.ColorScheme
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.service.LifeCalendarService
import java.time.LocalDate

/**
 * First-time setup activity for new users.
 * Collects birth date, expected lifespan, and preferred color scheme,
 * then saves preferences and launches the wallpaper picker.
 */
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private var selectedColorScheme: ColorScheme? = null
    private var selectedShapeId: String = "rounded_square"
    private var selectedScale: Float = 1.0f
    
    private lateinit var birthDatePicker: DatePicker
    private lateinit var lifespanInput: EditText
    private lateinit var chooseColorsButton: Button
    private lateinit var selectedSchemeText: TextView
    private lateinit var getStartedButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        preferencesManager = PreferencesManager(this)
        
        initViews()
        setupChooseColorsButton()
        setupGetStartedButton()
        
        // Set default selection to first scheme
        val defaultScheme = com.timehorizons.wallpaper.data.ColorSchemeProvider.getAllSchemes().first()
        selectColorScheme(defaultScheme)
    }
    
    private fun initViews() {
        birthDatePicker = findViewById(R.id.birthDatePicker)
        lifespanInput = findViewById(R.id.lifespanInput)
        chooseColorsButton = findViewById(R.id.chooseColorsButton)
        selectedSchemeText = findViewById(R.id.selectedSchemeText)
        getStartedButton = findViewById(R.id.getStartedButton)
        
        // Set default date to 25 years ago
        val defaultDate = LocalDate.now().minusYears(25)
        birthDatePicker.updateDate(
            defaultDate.year,
            defaultDate.monthValue - 1, // Month is 0-indexed in DatePicker
            defaultDate.dayOfMonth
        )
    }
    
    private fun setupChooseColorsButton() {
        chooseColorsButton.setOnClickListener {
            showColorSelectionBottomSheet()
        }
    }
    
    private fun showColorSelectionBottomSheet() {
        val bottomSheet = StyleSelectionBottomSheet.newInstance(
            selectedColorScheme?.id,
            selectedShapeId,
            selectedScale
        )
        
        bottomSheet.setOnStyleAppliedListener { scheme, shapeId, scale ->
            selectColorScheme(scheme)
            selectedShapeId = shapeId
            selectedScale = scale
        }
        
        bottomSheet.show(supportFragmentManager, "StyleSelectionBottomSheet")
    }
    
    private fun selectColorScheme(scheme: ColorScheme) {
        selectedColorScheme = scheme
        selectedSchemeText.text = "Selected: ${scheme.name}"
        selectedSchemeText.visibility = View.VISIBLE
    }
    
    private fun setupGetStartedButton() {
        getStartedButton.setOnClickListener {
            if (validateInput()) {
                saveAndFinish()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        // Validate birth date (must be in the past)
        val birthDate = LocalDate.of(
            birthDatePicker.year,
            birthDatePicker.month + 1, // Month is 0-indexed in DatePicker
            birthDatePicker.dayOfMonth
        )
        
        if (!birthDate.isBefore(LocalDate.now())) {
            Toast.makeText(this, R.string.birth_date_validation_error, Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate expected lifespan
        val lifespanText = lifespanInput.text.toString()
        val expectedLifespan = lifespanText.toIntOrNull()
        
        if (expectedLifespan == null || expectedLifespan <= 0) {
            Toast.makeText(this, R.string.lifespan_validation_error, Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Validate color scheme selected
        if (selectedColorScheme == null) {
            Toast.makeText(this, "Please select a color scheme", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun saveAndFinish() {
        val birthDate = LocalDate.of(
            birthDatePicker.year,
            birthDatePicker.month + 1, // Month is 0-indexed in DatePicker
            birthDatePicker.dayOfMonth
        )
        
        val expectedLifespan = lifespanInput.text.toString().toIntOrNull() ?: 90
        
        val preferences = UserPreferences(
            birthDate = birthDate,
            expectedLifespan = expectedLifespan,
            colorSchemeId = selectedColorScheme!!.id,
            isOnboardingComplete = true,
            unitShapeId = selectedShapeId,
            unitScale = selectedScale
        )
        
        preferencesManager.savePreferences(preferences)
        
        // Launch wallpaper picker
        launchWallpaperPicker()
        
        finish()
    }
    
    private fun launchWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@OnboardingActivity, LifeCalendarService::class.java)
            )
        }
        startActivity(intent)
    }
}
