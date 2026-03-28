package com.perspectivelive.wallpaper.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
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

    // Cache background color to reduce allocations in render loop
    private var lastBgUpdateMillis: Long = 0L
    private var cachedBgColor: Int = Color.BLACK

    // Health Connect formatting cache
    private var healthCache: Map<java.time.LocalDate, Float>? = null
    private var healthGoal: Float = 10000f
    private var healthMetric: String = "NONE"
    private var showStatOverlay: Boolean = false
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    init {
        gridConfig = calculateGrid()
        updateShapeDrawer()
    }

    fun updateHealthData(
        metric: String,
        goal: Float,
        showOverlay: Boolean,
        cache: Map<java.time.LocalDate, Float>?
    ) {
        this.healthMetric = metric
        this.healthGoal = goal
        this.showStatOverlay = showOverlay
        this.healthCache = cache
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
        updateBackgroundCache()
        canvas.drawColor(cachedBgColor)

        var dotIndex = 0
        val effectiveSize = gridConfig.dotSize * unitScale.coerceIn(0.5f, 1.0f)
        val offset = (gridConfig.dotSize - effectiveSize) / 2f
        val startX = gridConfig.offsetX
        val startY = gridConfig.offsetY
        val cellSize = gridConfig.dotSize + gridConfig.spacing

        // Precompute text styling if overlay is enabled
        if (showStatOverlay && healthMetric != "NONE") {
            textPaint.textSize = effectiveSize * 0.40f
            textPaint.color = cachedBgColor
            textPaint.alpha = 200 // Slight transparency for subtlety
        }

        for (row in 0 until gridConfig.rows) {
            for (col in 0 until gridConfig.columns) {
                if (dotIndex >= gridState.totalItems) break

                val x = startX + col * cellSize + offset
                val y = startY + row * cellSize + offset
                val itemDate = gridState.startDate.plusDays(dotIndex.toLong())

                val params = RenderItemParams(canvas, dotIndex, x, y, effectiveSize, itemDate, currentItemOpacity)
                drawGridItem(params)

                dotIndex++
            }
        }
    }

    private fun updateBackgroundCache() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBgUpdateMillis > 10_000L || lastBgUpdateMillis == 0L) {
            val hour = LocalDateTime.now().hour
            cachedBgColor = if (colorScheme.isDynamic) {
                ColorUtils.adaptBackgroundForTimeOfDay(colorScheme.backgroundColor, hour)
            } else {
                colorScheme.backgroundColor
            }
            lastBgUpdateMillis = currentTime
        }
    }

    private fun drawGridItem(p: RenderItemParams) {
        val color = when {
            p.dotIndex < gridState.pastItems -> colorScheme.pastYearsColor
            p.dotIndex == gridState.currentIndex -> colorScheme.currentYearColor
            else -> colorScheme.futureYearsColor
        }

        paint.color = color
        var drawnText: CharSequence? = null

        if (p.dotIndex == gridState.currentIndex) {
            paint.alpha = (255 * p.currentItemOpacity).toInt()
            if (showStatOverlay && healthMetric != "NONE" && healthCache != null) {
                healthCache?.get(p.itemDate)?.let { value ->
                    drawnText = formatHealthText(value, healthMetric)
                }
            }
        } else if (p.dotIndex < gridState.currentIndex && healthMetric != "NONE" && healthCache != null) {
            val value = healthCache?.get(p.itemDate) ?: 0f
            val progress = (value / healthGoal).coerceIn(0f, 1f)

            val originalAlpha = Color.alpha(color)
            val baseAlpha = (originalAlpha * 0.2f).toInt()
            paint.alpha = baseAlpha + ((originalAlpha - baseAlpha) * progress).toInt()

            if (showStatOverlay) drawnText = formatHealthText(value, healthMetric)
        } else {
            paint.alpha = Color.alpha(color)
        }

        shapeDrawer.draw(p.canvas, p.x, p.y, p.size, paint)

        drawnText?.let { text ->
            // Use StaticLayout to measure the exact bounding box of the text.
            // Using a huge width to force it to be a single line.
            val maxSingleLineWidth = 10000

            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxSingleLineWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, textPaint, maxSingleLineWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }

            // Calculate the actual exact width of the rendered text line.
            // Since it's forced on one line, max line width is the text width.
            var actualTextWidth = 0f
            for (i in 0 until staticLayout.lineCount) {
                val lineWidth = staticLayout.getLineWidth(i)
                if (lineWidth > actualTextWidth) {
                    actualTextWidth = lineWidth
                }
            }

            val actualTextHeight = staticLayout.height.toFloat()

            // Safe inner bounds (inscribed square)
            val safeBounds = p.size * 0.707f

            // Calculate scale to perfectly fit both dimensions
            val scaleX = if (actualTextWidth > safeBounds) safeBounds / actualTextWidth else 1.0f
            val scaleY = if (actualTextHeight > safeBounds) safeBounds / actualTextHeight else 1.0f
            val scaleFactor = kotlin.math.min(scaleX, scaleY)

            p.canvas.save()

            // Translate to the absolute center of the shape
            p.canvas.translate(p.x + p.size / 2f, p.y + p.size / 2f)

            if (scaleFactor < 1.0f) {
                p.canvas.scale(scaleFactor, scaleFactor)
            }

            // Offset the drawing operation by half the layout's width and height so it's perfectly centered
            p.canvas.translate(-actualTextWidth / 2f, -actualTextHeight / 2f)

            staticLayout.draw(p.canvas)
            p.canvas.restore()
        }
    }

    private fun formatHealthText(value: Float, metric: String): CharSequence {
        // No spaces, no newlines. Completely contiguous text strings to ensure single-line layout.
        val (text, suffix) = when (metric) {
            "STEPS" -> {
                if (value >= 1000) Pair(String.format("%.1f", value / 1000f), "k")
                else Pair(value.toInt().toString(), "")
            }
            "CALORIES" -> Pair(value.toInt().toString(), "kcal")
            "DISTANCE" -> Pair(String.format("%.1f", value), "km")
            "SLEEP" -> Pair(String.format("%.1f", value), "h")
            else -> Pair(value.toInt().toString(), "")
        }

        if (suffix.isEmpty()) return text

        val spannable = SpannableString(text + suffix)
        spannable.setSpan(
            RelativeSizeSpan(0.6f),
            text.length,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
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
