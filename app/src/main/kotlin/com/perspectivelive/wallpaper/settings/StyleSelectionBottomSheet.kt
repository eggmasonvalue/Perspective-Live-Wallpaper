package com.perspectivelive.wallpaper.settings

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.PreferencesManager

/**
 * Material 3 Modal Bottom Sheet for style selection (Shape, Size, Color, Breathing Cycle).
 */
class StyleSelectionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var colorCardAdapter: ColorCardAdapter
    
    private var selectedScheme: ColorScheme? = null
    private var selectedShapeId: String = "rounded_square"
    private var selectedScale: Float = 1.0f
    private var selectedPaddingScale: Float = 0.05f
    private var selectedPulsePeriodMs: Long = 2000L
    
    private var onStyleApplied: ((ColorScheme, String, Float, Float, Long) -> Unit)? = null

    private val customColorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // User created custom colors, update grid
            preferencesManager.getCustomColors()?.let { customColors ->
                val customScheme = ColorSchemeProvider.createCustomColorScheme(customColors)
                colorCardAdapter.updateCustomScheme(customScheme)
                selectedScheme = customScheme
                colorCardAdapter.setSelectedScheme(customScheme.id)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_style_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        // Initialize state from args
        arguments?.let { args ->
            selectedShapeId = args.getString(ARG_INITIAL_SHAPE_ID, "rounded_square")
            selectedScale = args.getFloat(ARG_INITIAL_SCALE, 1.0f)
            selectedPaddingScale = args.getFloat(ARG_INITIAL_PADDING_SCALE, 0.05f)
            selectedPulsePeriodMs = args.getLong(ARG_INITIAL_PULSE_PERIOD_MS, 2000L)
            
            // Scheme will be set in setupColorGrid
        }

        setupShapeToggle(view)
        setupSizeSlider(view)
        setupPaddingSlider(view)
        setupPulsePeriodSlider(view)
        setupColorGrid(view)
        setupButtons(view)
    }

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.7).toInt() 
            }
        }
        
        return dialog
    }

    private fun setupShapeToggle(view: View) {
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.shapeToggleGroup)
        
        // Select initial button
        val initialBtnId = when (selectedShapeId) {
            "circle" -> R.id.btnShapeCircle
            "rhombus", "square" -> R.id.btnShapeRhombus
            else -> R.id.btnShapeRounded
        }
        toggleGroup.check(initialBtnId)
        
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedShapeId = when (checkedId) {
                    R.id.btnShapeCircle -> "circle"
                    R.id.btnShapeRhombus -> "rhombus"
                    else -> "rounded_square"
                }
            }
        }
    }

    private fun setupSizeSlider(view: View) {
        val slider = view.findViewById<Slider>(R.id.sizeSlider)
        val valueText = view.findViewById<TextView>(R.id.sizeValueText)
        
        if (slider != null) {
            slider.value = selectedScale.coerceIn(0.5f, 1.0f)
            valueText?.text = "${(selectedScale * 100).toInt()}%"
            
            slider.addOnChangeListener { _, value, _ ->
                selectedScale = value
                valueText?.text = "${(value * 100).toInt()}%"
            }
        }
    }

    private fun setupPaddingSlider(view: View) {
        val slider = view.findViewById<Slider>(R.id.paddingSlider)
        val valueText = view.findViewById<TextView>(R.id.paddingValueText)
        
        if (slider != null) {
            slider.value = selectedPaddingScale
            valueText?.text = "${(selectedPaddingScale * 100).toInt()}%"
            
            slider.addOnChangeListener { _, value, _ ->
                selectedPaddingScale = value
                valueText?.text = "${(value * 100).toInt()}%"
            }
        }
    }

    private fun setupPulsePeriodSlider(view: View) {
        val slider = view.findViewById<Slider>(R.id.blinkPeriodSlider)
        val valueText = view.findViewById<TextView>(R.id.blinkPeriodValueText)
        
        if (slider != null) {
            slider.value = selectedPulsePeriodMs.toFloat().coerceIn(500f, 5000f)
            valueText?.text = "${selectedPulsePeriodMs}ms"
            
            slider.addOnChangeListener { _, value, _ ->
                selectedPulsePeriodMs = value.toLong()
                valueText?.text = "${selectedPulsePeriodMs}ms"
            }
        }
    }

    private fun setupColorGrid(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.colorCardsRecyclerView)

        // Get all preset schemes
        val schemes = ColorSchemeProvider.getAllSchemes().toMutableList()

        // Add custom scheme if it exists
        if (preferencesManager.hasCustomColors()) {
            preferencesManager.getCustomColors()?.let { customColors ->
                val customScheme = ColorSchemeProvider.createCustomColorScheme(customColors)
                schemes.add(customScheme)
            }
        }

        // Set up adapter
        colorCardAdapter = ColorCardAdapter(
            schemes = schemes,
            onSchemeSelected = { scheme ->
                selectedScheme = scheme
            },
            onCreateCustom = {
                launchCustomColorPicker()
            }
        )

        // Get initial selection from arguments
        val initialSchemeId = arguments?.getString(ARG_INITIAL_SCHEME_ID)
        if (initialSchemeId != null) {
            selectedScheme = schemes.find { it.id == initialSchemeId }
            colorCardAdapter.setSelectedScheme(initialSchemeId)
        } else {
             // Fallback if needed, though mostly handled by caller passing ID
             selectedScheme = schemes.find { it.id == "dark" }
        }

        val spanCount = 3
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.adapter = colorCardAdapter
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }

        view.findViewById<Button>(R.id.applyButton).setOnClickListener {
            selectedScheme?.let { scheme ->
                onStyleApplied?.invoke(scheme, selectedShapeId, selectedScale, selectedPaddingScale, selectedPulsePeriodMs)
            }
            dismiss()
        }
    }

    private fun launchCustomColorPicker() {
        val intent = Intent(requireContext(), CustomColorActivity::class.java)
        customColorLauncher.launch(intent)
    }

    /**
     * Sets the callback for when style is applied.
     */
    fun setOnStyleAppliedListener(listener: (ColorScheme, String, Float, Float, Long) -> Unit) {
        onStyleApplied = listener
    }

    companion object {
        private const val ARG_INITIAL_SCHEME_ID = "initial_scheme_id"
        private const val ARG_INITIAL_SHAPE_ID = "initial_shape_id"
        private const val ARG_INITIAL_SCALE = "initial_scale"
        private const val ARG_INITIAL_PADDING_SCALE = "initial_padding_scale"
        private const val ARG_INITIAL_PULSE_PERIOD_MS = "initial_pulse_period_ms"

        /**
         * Creates a new instance of the style sheet with initial settings.
         */
        fun newInstance(
            initialSchemeId: String?,
            initialShapeId: String,
            initialScale: Float,
            initialPaddingScale: Float = 0.05f,
            initialPulsePeriodMs: Long = 2000L
        ): StyleSelectionBottomSheet {
            return StyleSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    if (initialSchemeId != null) {
                        putString(ARG_INITIAL_SCHEME_ID, initialSchemeId)
                    }
                    putString(ARG_INITIAL_SHAPE_ID, initialShapeId)
                    putFloat(ARG_INITIAL_SCALE, initialScale)
                    putFloat(ARG_INITIAL_PADDING_SCALE, initialPaddingScale)
                    putLong(ARG_INITIAL_PULSE_PERIOD_MS, initialPulsePeriodMs)
                }
            }
        }
    }
}
