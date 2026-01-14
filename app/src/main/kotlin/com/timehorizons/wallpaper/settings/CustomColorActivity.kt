package com.timehorizons.wallpaper.settings

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.timehorizons.wallpaper.R
import com.timehorizons.wallpaper.data.CustomColorScheme
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.service.LifeCalendarService

/**
 * Activity for creating a custom color scheme.
 * User selects three colors: background, past/future, and current year accent.
 */
class CustomColorActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    
    private lateinit var schemeNameInput: TextInputEditText
    private lateinit var backgroundColorButton: Button
    private lateinit var pastFutureColorButton: Button
    private lateinit var currentColorButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    // Color selections (initialized to defaults)
    private var backgroundColor = 0xFF000000.toInt() // Black
    private var pastFutureColor = 0xFFFFFFFF.toInt() // White
    private var currentColor = 0xFFFF0000.toInt() // Red

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_color)
        
        preferencesManager = PreferencesManager(this)
        
        initViews()
        loadExistingColors()
        setupColorButtons()
        setupActionButtons()
        updatePreview()
    }

    private fun initViews() {
        schemeNameInput = findViewById(R.id.schemeNameInput)
        backgroundColorButton = findViewById(R.id.backgroundColorButton)
        pastFutureColorButton = findViewById(R.id.pastFutureColorButton)
        currentColorButton = findViewById(R.id.currentColorButton)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun loadExistingColors() {
        // Load existing custom colors if they exist
        val customColors = preferencesManager.getCustomColors()
        if (customColors != null) {
            schemeNameInput.setText(customColors.name)
            backgroundColor = customColors.backgroundColor
            pastFutureColor = customColors.pastFutureColor
            currentColor = customColors.currentColor
        }
    }

    private fun setupColorButtons() {
        backgroundColorButton.setOnClickListener {
            showColorPicker(backgroundColor) { color ->
                backgroundColor = color
                updatePreview()
            }
        }

        pastFutureColorButton.setOnClickListener {
            showColorPicker(pastFutureColor) { color ->
                pastFutureColor = color
                updatePreview()
            }
        }

        currentColorButton.setOnClickListener {
            showColorPicker(currentColor) { color ->
                currentColor = color
                updatePreview()
            }
        }
    }

    private fun setupActionButtons() {
        saveButton.setOnClickListener {
            saveCustomColors()
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun showColorPicker(initialColor: Int, onColorSelected: (Int) -> Unit) {
        val dialog = ColorPickerDialog(this, initialColor) { color ->
            onColorSelected(color)
        }
        dialog.show()
    }

    private fun updatePreview() {
        // Update button preview colors
        backgroundColorButton.setBackgroundColor(backgroundColor)
        pastFutureColorButton.setBackgroundColor(pastFutureColor)
        currentColorButton.setBackgroundColor(currentColor)
        
        // Adjust text color for visibility
        backgroundColorButton.setTextColor(getContrastColor(backgroundColor))
        pastFutureColorButton.setTextColor(getContrastColor(pastFutureColor))
        currentColorButton.setTextColor(getContrastColor(currentColor))
    }

    private fun getContrastColor(color: Int): Int {
        // Simple luminance calculation
        val luminance = (0.299 * Color.red(color) + 
                        0.587 * Color.green(color) + 
                        0.114 * Color.blue(color))
        return if (luminance > 128) Color.BLACK else Color.WHITE
    }

    private fun saveCustomColors() {
        val schemeName = schemeNameInput.text?.toString()?.trim()
        if (schemeName.isNullOrBlank()) {
            Toast.makeText(this, "Please enter a scheme name", Toast.LENGTH_SHORT).show()
            return
        }
        
        val customColors = CustomColorScheme(
            name = schemeName,
            backgroundColor = backgroundColor,
            pastFutureColor = pastFutureColor,
            currentColor = currentColor
        )
        
        preferencesManager.saveCustomColors(customColors)
        
        Toast.makeText(this, "Custom colors saved!", Toast.LENGTH_SHORT).show()
        
        // Return result to indicate custom scheme was created
        setResult(RESULT_OK)
        finish()
    }
}
