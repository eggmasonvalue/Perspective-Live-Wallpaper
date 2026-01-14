package com.timehorizons.wallpaper.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.timehorizons.wallpaper.data.ColorScheme
import com.timehorizons.wallpaper.data.GridConfig
import com.timehorizons.wallpaper.data.LifeState
import com.timehorizons.wallpaper.utils.ColorUtils
import java.time.LocalDateTime

/**
 * Renders the life calendar grid on a Canvas.
 * Handles drawing the background, all year dots, and applying the pulse animation.
 */
class CanvasRenderer(
    private var lifeState: LifeState,
    private val colorScheme: ColorScheme,
    private val screenWidth: Int,
    private val screenHeight: Int
) {
    private val gridConfig: GridConfig
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Style settings
    private var unitShapeId: String = "rounded_square"
    private var unitScale: Float = 1.0f
    
    init {
        gridConfig = GridCalculator.calculateGridLayout(
            totalDots = lifeState.totalYears,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
    }
    
    /**
     * Renders the complete life calendar to the canvas.
     *
     * @param canvas The canvas to draw on
     * @param currentYearOpacity Opacity value for the current year dot (from PulseAnimator)
     */
    fun render(canvas: Canvas, currentYearOpacity: Float) {
        // Draw background with time-of-day adaptation
        val hour = LocalDateTime.now().hour
        val bgColor = if (colorScheme.isDynamic) {
            ColorUtils.adaptBackgroundForTimeOfDay(colorScheme.backgroundColor, hour)
        } else {
            colorScheme.backgroundColor
        }
        canvas.drawColor(bgColor)
        
        // Draw all dots
        var dotIndex = 0
        
        // Calculate effective dot size based on scale
        // unitScale 1.0 = gridConfig.dotSize
        // unitScale 0.5 = gridConfig.dotSize * 0.5
        val effectiveSize = gridConfig.dotSize * unitScale.coerceIn(0.5f, 1.0f)
        val offset = (gridConfig.dotSize - effectiveSize) / 2f
        
        for (row in 0 until gridConfig.rows) {
            for (col in 0 until gridConfig.columns) {
                if (dotIndex >= lifeState.totalYears) break
                
                // Top-left of the grid cell
                val cellX = gridConfig.offsetX + col * (gridConfig.dotSize + gridConfig.spacing)
                val cellY = gridConfig.offsetY + row * (gridConfig.dotSize + gridConfig.spacing)
                
                // Centered drawing coordinates
                val x = cellX + offset
                val y = cellY + offset
                
                val color = when {
                    dotIndex < lifeState.yearsLived -> colorScheme.pastYearsColor
                    dotIndex == lifeState.currentYearIndex -> colorScheme.currentYearColor
                    else -> colorScheme.futureYearsColor
                }
                
                paint.color = color
                
                // Apply pulse opacity only to current year
                if (dotIndex == lifeState.currentYearIndex) {
                    paint.alpha = (255 * currentYearOpacity).toInt()
                } else {
                    paint.alpha = Color.alpha(color)
                }
                
                drawShape(canvas, x, y, effectiveSize)
                
                dotIndex++
            }
        }
    }
    
    private fun drawShape(canvas: Canvas, x: Float, y: Float, size: Float) {
        when (unitShapeId) {
            "circle" -> {
                val radius = size / 2f
                canvas.drawCircle(x + radius, y + radius, radius, paint)
            }
            "square" -> {
                canvas.drawRect(x, y, x + size, y + size, paint)
            }
            "squircle" -> {
                // Approximate squircle with very rounded rect (radius ~ 25%) 
                // or use a path if needed. For performance, large radius rect is close enough visually for small dots
                // A true squircle has 'hyper-rounded' corners.
                // Using a corner radius of 50% makes a circle.
                // Using a corner radius of 0% makes a square.
                // Squircle is often approximated by ~20-25% radius but with "smoother" corners.
                // Android's drawRoundRect is just circular corners.
                // For "Squircle", we'll use a corner radius of 20% of size.
                val radius = size * 0.22f
                canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
            }
            "rounded_square" -> {
                // Standard rounded square, match gridConfig or use fixed ratio
                // Prior implementation used gridConfig.cornerRadius.
                // Let's use a standard visual ratio, e.g. 15%
                val radius = size * 0.15f
                canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
            }
            else -> {
                // Default to rounded square
                val radius = size * 0.15f
                canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
            }
        }
    }
    
    /**
     * Updates the life state for the renderer.
     * Call this when the user's age changes (e.g., on birthday).
     */
    fun updateLifeState(newLifeState: LifeState) {
        lifeState = newLifeState
    }
    
    /**
     * Updates the visual style settings.
     */
    fun updateStyle(shapeId: String, scale: Float) {
        this.unitShapeId = shapeId
        this.unitScale = scale
    }
}
