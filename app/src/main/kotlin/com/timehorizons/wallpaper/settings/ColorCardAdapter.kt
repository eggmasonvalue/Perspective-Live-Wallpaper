package com.timehorizons.wallpaper.settings

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.timehorizons.wallpaper.R
import com.timehorizons.wallpaper.data.ColorScheme

/**
 * Adapter for displaying color schemes as cards in a grid.
 * Shows visual preview of colors and handles selection state.
 */
class ColorCardAdapter(
    private val schemes: MutableList<ColorScheme>,
    private val onSchemeSelected: (ColorScheme) -> Unit,
    private val onCreateCustom: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedSchemeId: String? = null
    private val VIEW_TYPE_SCHEME = 0
    private val VIEW_TYPE_CREATE_CUSTOM = 1

    /**
     * ViewHolder for color scheme cards.
     */
    class SchemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.colorCard)
        val previewContainer: View = view.findViewById(R.id.previewContainer)
        val boxTopLeft: View = view.findViewById(R.id.boxTopLeft)
        val boxTopRight: View = view.findViewById(R.id.boxTopRight)
        val boxBottomLeft: View = view.findViewById(R.id.boxBottomLeft)
        val boxBottomRight: View = view.findViewById(R.id.boxBottomRight)
        val schemeName: TextView = view.findViewById(R.id.schemeName)
        val checkmark: TextView = view.findViewById(R.id.checkmarkIcon)
    }

    /**
     * ViewHolder for "Create Custom" card.
     */
    class CreateCustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.colorCard)
        val schemeName: TextView = view.findViewById(R.id.schemeName)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < schemes.size) VIEW_TYPE_SCHEME else VIEW_TYPE_CREATE_CUSTOM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SCHEME -> {
                val view = inflater.inflate(R.layout.item_color_card, parent, false)
                SchemeViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_color_card, parent, false)
                CreateCustomViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SchemeViewHolder -> bindSchemeCard(holder, position)
            is CreateCustomViewHolder -> bindCreateCustomCard(holder)
        }
    }

    private fun bindSchemeCard(holder: SchemeViewHolder, position: Int) {
        val scheme = schemes[position]

        // Set preview container background color
        setRoundedBackground(holder.previewContainer, scheme.backgroundColor, 12f)

        // Set 4 boxes in 2x2 grid: 3 past/future, 1 current (bottom-right)
        val dotRadius = 8f
        setRoundedBackground(holder.boxTopLeft, scheme.pastYearsColor, dotRadius)
        setRoundedBackground(holder.boxTopRight, scheme.pastYearsColor, dotRadius)
        setRoundedBackground(holder.boxBottomLeft, scheme.pastYearsColor, dotRadius)
        setRoundedBackground(holder.boxBottomRight, scheme.currentYearColor, dotRadius)

        // Set scheme name
        holder.schemeName.text = scheme.name

        // Set selected state
        val isSelected = scheme.id == selectedSchemeId
        holder.checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.card.strokeWidth = if (isSelected) 4 else 0
        holder.card.elevation = if (isSelected) 8f else 2f

        // Click handler
        holder.card.setOnClickListener {
            selectedSchemeId = scheme.id

            // Notify adapter to update selection states
            notifyDataSetChanged()

            onSchemeSelected(scheme)
        }
    }

    private fun bindCreateCustomCard(holder: CreateCustomViewHolder) {
        // Hide preview elements for create custom card
        holder.itemView.findViewById<View>(R.id.previewContainer)?.visibility = View.GONE

        holder.schemeName.text = "+ Create Custom"
        holder.schemeName.textSize = 16f

        // Make the entire card area clickable
        holder.card.minimumHeight = 180

        holder.card.setOnClickListener {
            onCreateCustom()
        }
    }

    override fun getItemCount(): Int {
        // Schemes + 1 for "Create Custom" card
        return schemes.size + 1
    }

    /**
     * Updates the selected scheme.
     */
    fun setSelectedScheme(schemeId: String) {
        selectedSchemeId = schemeId
        notifyDataSetChanged()
    }

    /**
     * Adds or updates custom scheme in the list.
     */
    fun updateCustomScheme(customScheme: ColorScheme) {
        val existingIndex = schemes.indexOfFirst { it.isCustom }
        if (existingIndex != -1) {
            schemes[existingIndex] = customScheme
            notifyItemChanged(existingIndex)
        } else {
            schemes.add(customScheme)
            notifyItemInserted(schemes.size - 1)
        }
    }

    private fun setRoundedBackground(view: View, color: Int, cornerRadiusDp: Float) {
        val radiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cornerRadiusDp,
            view.resources.displayMetrics
        )
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = radiusPx
        drawable.setColor(color)
        view.background = drawable
    }
}
