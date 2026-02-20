package com.timehorizons.wallpaper.settings

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import com.timehorizons.wallpaper.R
import java.util.Locale

/**
 * Simple color picker dialog using RGB sliders.
 * Returns the selected color via callback.
 */
class ColorPickerDialog(
    context: Context,
    private val initialColor: Int = Color.BLACK,
    private val onColorSelected: (Int) -> Unit
) : Dialog(context) {

    private lateinit var redSeekBar: SeekBar
    private lateinit var greenSeekBar: SeekBar
    private lateinit var blueSeekBar: SeekBar
    private lateinit var colorPreview: View
    private lateinit var hexText: TextView
    private lateinit var selectButton: Button
    private lateinit var cancelButton: Button

    private var currentColor: Int = initialColor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_picker)
        
        initViews()
        setupColorComponents()
        setupButtons()
        
        // Initialize with the initial color
        setColor(initialColor)
    }

    private fun initViews() {
        redSeekBar = findViewById(R.id.redSeekBar)
        greenSeekBar = findViewById(R.id.greenSeekBar)
        blueSeekBar = findViewById(R.id.blueSeekBar)
        colorPreview = findViewById(R.id.colorPreview)
        hexText = findViewById(R.id.hexText)
        selectButton = findViewById(R.id.selectButton)
        cancelButton = findViewById(R.id.cancelButton)
    }

    private fun setupColorComponents() {
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        redSeekBar.max = 255
        greenSeekBar.max = 255
        blueSeekBar.max = 255

        redSeekBar.setOnSeekBarChangeListener(seekBarListener)
        greenSeekBar.setOnSeekBarChangeListener(seekBarListener)
        blueSeekBar.setOnSeekBarChangeListener(seekBarListener)
    }

    private fun setupButtons() {
        selectButton.setOnClickListener {
            onColorSelected(currentColor)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setColor(color: Int) {
        redSeekBar.progress = Color.red(color)
        greenSeekBar.progress = Color.green(color)
        blueSeekBar.progress = Color.blue(color)
        updateColor()
    }

    private fun updateColor() {
        val red = redSeekBar.progress
        val green = greenSeekBar.progress
        val blue = blueSeekBar.progress

        currentColor = Color.argb(255, red, green, blue)
        colorPreview.setBackgroundColor(currentColor)
        
        // Update hex text
        val hexColor = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)
        hexText.text = hexColor
    }
}
