package com.perspectivelive.wallpaper.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorScheme

/**
 * RecyclerView adapter for displaying and selecting color schemes.
 * 
 * @property schemes List of available color schemes
 * @property onSchemeSelected Callback invoked when a scheme is selected
 */
class ColorSchemeAdapter(
    private val schemes: List<ColorScheme>,
    private val onSchemeSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorSchemeAdapter.ViewHolder>() {
    
    private var selectedSchemeId: String = schemes.firstOrNull()?.id ?: "dark"
    
    /**
     * ViewHolder for color scheme items.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorPreview: View = view.findViewById(R.id.colorPreview)
        val schemeName: TextView = view.findViewById(R.id.schemeName)
        val schemeDescription: TextView = view.findViewById(R.id.schemeDescription)
        val radioButton: RadioButton = view.findViewById(R.id.radioButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_scheme, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scheme = schemes[position]
        
        // Set color preview to the past years color (primary color of the scheme)
        holder.colorPreview.setBackgroundColor(scheme.pastYearsColor)
        holder.schemeName.text = scheme.name
        holder.schemeDescription.text = if (scheme.isDynamic) {
            "Adapts to time of day"
        } else {
            "Static colors"
        }
        holder.radioButton.isChecked = (scheme.id == selectedSchemeId)
        
        holder.itemView.setOnClickListener {
            val previousSelected = selectedSchemeId
            selectedSchemeId = scheme.id
            
            // Notify to update radio buttons for both items
            val previousIndex = schemes.indexOfFirst { it.id == previousSelected }
            if (previousIndex != -1) {
                notifyItemChanged(previousIndex)
            }
            notifyItemChanged(position)
            
            onSchemeSelected(scheme.id)
        }
    }
    
    override fun getItemCount(): Int = schemes.size
    
    /**
     * Sets the currently selected scheme and updates the UI.
     */
    fun setSelectedScheme(schemeId: String) {
        val previousSelected = selectedSchemeId
        selectedSchemeId = schemeId
        
        val previousIndex = schemes.indexOfFirst { it.id == previousSelected }
        val newIndex = schemes.indexOfFirst { it.id == schemeId }
        
        if (previousIndex != -1) {
            notifyItemChanged(previousIndex)
        }
        if (newIndex != -1) {
            notifyItemChanged(newIndex)
        }
    }
    
    /**
     * Returns the currently selected scheme ID.
     */
    fun getSelectedSchemeId(): String = selectedSchemeId
}
