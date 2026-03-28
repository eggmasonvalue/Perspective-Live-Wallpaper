package com.perspectivelive.wallpaper.settings

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorScheme

class ColorCardAdapter(
    private val schemes: MutableList<ColorScheme>,
    private val onSchemeSelected: (ColorScheme) -> Unit,
    private val onCreateCustom: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedSchemeId: String? = null
    private val viewTypeScheme = 0
    private val viewTypeCreateCustom = 1

    class SchemeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.colorCard)
        val previewContainer: View = view.findViewById(R.id.previewContainer)
        val previewCanvas: PreviewCanvasView = view.findViewById(R.id.previewCanvas)
        val schemeName: TextView = view.findViewById(R.id.schemeName)
        val checkmark: TextView = view.findViewById(R.id.checkmarkIcon)
    }

    class CreateCustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.colorCard)
        val schemeName: TextView = view.findViewById(R.id.schemeName)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < schemes.size) viewTypeScheme else viewTypeCreateCustom
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            viewTypeScheme -> {
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

        // Set the lightweight canvas grid preview
        holder.previewCanvas.setColors(scheme.pastYearsColor, scheme.currentYearColor, scheme.futureYearsColor)

        holder.schemeName.text = scheme.name

        // Set selected state
        val isSelected = scheme.id == selectedSchemeId
        holder.checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.card.strokeWidth = if (isSelected) 4 else 0
        holder.card.elevation = if (isSelected) 8f else 2f

        // Click handler
        holder.card.setOnClickListener {
            selectedSchemeId = scheme.id
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
        return schemes.size + 1
    }

    fun setSelectedScheme(schemeId: String) {
        selectedSchemeId = schemeId
        notifyDataSetChanged()
    }

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
