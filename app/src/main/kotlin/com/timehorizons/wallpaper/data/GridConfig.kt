package com.timehorizons.wallpaper.data

/**
 * Data class holding the calculated grid layout configuration.
 *
 * @property rows Number of rows in the grid
 * @property columns Number of columns in the grid
 * @property dotSize Size of each dot in pixels (width and height)
 * @property spacing Spacing between dots in pixels
 * @property offsetX Horizontal offset to center the grid in pixels
 * @property offsetY Vertical offset to center the grid in pixels
 * @property cornerRadius Corner radius for the rounded squares in pixels
 */
data class GridConfig(
    val rows: Int,
    val columns: Int,
    val dotSize: Float,
    val spacing: Float,
    val offsetX: Float,
    val offsetY: Float,
    val cornerRadius: Float
)
