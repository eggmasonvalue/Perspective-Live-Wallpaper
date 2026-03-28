package com.perspectivelive.wallpaper.rendering

import com.perspectivelive.wallpaper.data.GridConfig
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Utility object for calculating the optimal grid layout for displaying dots
 * on screen, considering the screen's aspect ratio.
 */
object GridCalculator {

    /**
     * Calculates the optimal grid layout for a given number of dots on a screen.
     *
     * @param totalDots Total number of dots to display
     * @param screenWidth Width of the screen in pixels
     * @param screenHeight Height of the screen in pixels
     * @param marginPercent Percentage of screen to use as margin (default 5%)
     * @return A GridConfig with optimal rows, columns, dot size, spacing, and offsets
     */
    fun calculateGridLayout(
        totalDots: Int,
        screenWidth: Int,
        screenHeight: Int,
        marginPercent: Float = 0.05f
    ): GridConfig {
        // Safeguard against invalid inputs
        if (totalDots <= 0 || screenWidth <= 0 || screenHeight <= 0) {
            return GridConfig(
                rows = 1,
                columns = 1,
                dotSize = 1f,
                spacing = 0f,
                offsetX = 0f,
                offsetY = 0f,
                cornerRadius = 0f
            )
        }

        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

        // Find optimal rows/columns that match screen aspect ratio
        var bestRows = 1
        var bestCols = totalDots
        var bestRatioDiff = Float.MAX_VALUE

        // Iterate by columns to get exact rows
        for (cols in 1..totalDots) {
            val rows = ceil(totalDots.toFloat() / cols).toInt()
            val gridRatio = cols.toFloat() / rows.toFloat()
            val ratioDiff = abs(gridRatio - aspectRatio)

            if (ratioDiff < bestRatioDiff) {
                bestRatioDiff = ratioDiff
                bestRows = rows
                bestCols = cols
            }

            if (cols > sqrt(totalDots.toDouble()) * 2.0) break
        }

        // Calculate dot size with margins
        val horizontalMargin = (screenWidth * marginPercent).toInt()
        val verticalMargin = (screenHeight * marginPercent).toInt()

        val usableWidth = screenWidth - (2 * horizontalMargin)
        val usableHeight = screenHeight - (2 * verticalMargin)

        // Calculate maximum dot size that fits
        val spacingPercent = 0.1f  // 10% spacing between dots
        val maxDotWidth = usableWidth.toFloat() / (bestCols + (bestCols - 1) * spacingPercent)
        val maxDotHeight = usableHeight.toFloat() / (bestRows + (bestRows - 1) * spacingPercent)

        var dotSize = min(maxDotWidth, maxDotHeight)

        // Safeguard against extremely small or negative dot size
        if (dotSize < 1f) dotSize = 1f

        val spacing = dotSize * spacingPercent

        // The key to exact vertical centering:
        // 1. Grid width/height derived directly from bestCols/bestRows
        // 2. Offsets derived directly from screenWidth/screenHeight, taking the exact empty space
        val gridWidth = (bestCols * dotSize) + ((bestCols - 1) * spacing)
        val gridHeight = (bestRows * dotSize) + ((bestRows - 1) * spacing)

        val offsetX = (screenWidth - gridWidth) / 2f
        val offsetY = (screenHeight - gridHeight) / 2f

        return GridConfig(
            rows = bestRows,
            columns = bestCols,
            dotSize = dotSize,
            spacing = spacing,
            offsetX = offsetX,
            offsetY = offsetY,
            cornerRadius = dotSize * 0.15f  // 15% of dot size
        )
    }
}
