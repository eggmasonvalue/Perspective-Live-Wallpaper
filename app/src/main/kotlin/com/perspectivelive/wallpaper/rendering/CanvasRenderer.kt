package com.perspectivelive.wallpaper.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.GridConfig
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.utils.ColorUtils
import java.time.LocalDateTime

/**
 * Renders the grid on a Canvas.
 * Optimized with strategy pattern for shape drawing.
 */
class CanvasRenderer(
    private var gridState: GridState,
    val colorScheme: ColorScheme,
    private val screenWidth: Int,
    private val screenHeight: Int
) {
    private var gridConfig: GridConfig
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Style settings
    private var unitShapeId: String = "rounded_square"
    private var unitScale: Float = 1.0f
    private var containerPaddingScale: Float = 0.05f

    private var shapeDrawer: ShapeDrawer = RoundedSquareDrawer()

    init {
        gridConfig = calculateGrid()
        updateShapeDrawer()
    }

    private fun calculateGrid(): GridConfig {
        return GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPercent = containerPaddingScale
        )
    }

    fun render(canvas: Canvas, currentItemOpacity: Float) {
        val hour = LocalDateTime.now().hour
        val bgColor = if (colorScheme.isDynamic) {
            ColorUtils.adaptBackgroundForTimeOfDay(colorScheme.backgroundColor, hour)
        } else {
            colorScheme.backgroundColor
        }
        canvas.drawColor(bgColor)

        var dotIndex = 0
        val effectiveSize = gridConfig.dotSize * unitScale.coerceIn(0.5f, 1.0f)
        val offset = (gridConfig.dotSize - effectiveSize) / 2f
        val startX = gridConfig.offsetX
        val startY = gridConfig.offsetY
        val cellSize = gridConfig.dotSize + gridConfig.spacing

        for (row in 0 until gridConfig.rows) {
            for (col in 0 until gridConfig.columns) {
                if (dotIndex >= gridState.totalItems) break

                val x = startX + col * cellSize + offset
                val y = startY + row * cellSize + offset

                val color = when {
                    dotIndex < gridState.pastItems -> colorScheme.pastYearsColor
                    dotIndex == gridState.currentIndex -> colorScheme.currentYearColor
                    else -> colorScheme.futureYearsColor
                }

                paint.color = color

                if (dotIndex == gridState.currentIndex) {
                    paint.alpha = (255 * currentItemOpacity).toInt()
                } else {
                    paint.alpha = Color.alpha(color)
                }

                shapeDrawer.draw(canvas, x, y, effectiveSize, paint)

                dotIndex++
            }
        }
    }

    fun updateGridState(newState: GridState) {
        gridState = newState
        gridConfig = calculateGrid()
    }

    fun updateStyle(shapeId: String, scale: Float, paddingScale: Float = 0.05f) {
        this.unitShapeId = shapeId
        this.unitScale = scale
        this.containerPaddingScale = paddingScale
        gridConfig = calculateGrid()
        updateShapeDrawer()
    }

    private fun updateShapeDrawer() {
        shapeDrawer = when (unitShapeId) {
            "circle" -> CircleDrawer()
            "rhombus", "square" -> RhombusDrawer()
            "squircle" -> SquircleDrawer()
            else -> RoundedSquareDrawer()
        }
    }

    private interface ShapeDrawer {
        fun draw(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint)
    }

    private class CircleDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
            val radius = size / 2f
            canvas.drawCircle(x + radius, y + radius, radius, paint)
        }
    }

    private class RhombusDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
            canvas.save()
            canvas.rotate(45f, x + size / 2f, y + size / 2f)
            val scale = 0.707f
            val center = size / 2f
            val halfScaled = (size * scale) / 2f
            canvas.drawRect(x + center - halfScaled, y + center - halfScaled,
                            x + center + halfScaled, y + center + halfScaled, paint)
            canvas.restore()
        }
    }

    private class SquircleDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
            val radius = size * 0.22f
            canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
        }
    }

    private class RoundedSquareDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
            val radius = size * 0.15f
            canvas.drawRoundRect(x, y, x + size, y + size, radius, radius, paint)
        }
    }
}
