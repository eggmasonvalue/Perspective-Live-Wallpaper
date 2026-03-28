package com.perspectivelive.wallpaper.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * A lightweight custom view that draws a dense grid (e.g., 16x9) to simulate the wallpaper.
 */
class PreviewCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var pastColor: Int = 0
    var currentColor: Int = 0
    var futureColor: Int = 0
    var columns: Int = 9
    var rows: Int = 16
    var currentItemIndex: Int = (columns * rows) / 2 // Default to middle

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 4f // Internal padding between shapes
        val totalWidth = width.toFloat()
        val totalHeight = height.toFloat()

        val cellSizeX = totalWidth / columns
        val cellSizeY = totalHeight / rows
        val cellSize = minOf(cellSizeX, cellSizeY)

        // Center the grid in the view
        val startX = (totalWidth - (cellSize * columns)) / 2f
        val startY = (totalHeight - (cellSize * rows)) / 2f

        val shapeSize = cellSize - padding
        val cornerRadius = shapeSize * 0.2f

        var dotIndex = 0
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val x = startX + c * cellSize + padding / 2f
                val y = startY + r * cellSize + padding / 2f

                paint.color = when {
                    dotIndex < currentItemIndex -> pastColor
                    dotIndex == currentItemIndex -> currentColor
                    else -> futureColor
                }

                // Simply draw rounded squares for the preview (fastest)
                canvas.drawRoundRect(x, y, x + shapeSize, y + shapeSize, cornerRadius, cornerRadius, paint)
                dotIndex++
            }
        }
    }

    fun setColors(past: Int, current: Int, future: Int) {
        pastColor = past
        currentColor = current
        futureColor = future
        invalidate()
    }
}
