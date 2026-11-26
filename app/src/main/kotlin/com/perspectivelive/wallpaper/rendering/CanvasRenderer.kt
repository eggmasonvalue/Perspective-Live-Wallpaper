package com.perspectivelive.wallpaper.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.GridConfig
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UnitShape
import com.perspectivelive.wallpaper.utils.ColorUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

private const val FULL_ALPHA = 255
private const val THOUSAND_STEPS = 1000
private const val RELATIVE_SIZE_SPAN_PROPORTION = 0.6f

/**
 * Precomputed layout and text state for a single grid dot.
 * Pre-allocated during configuration/health updates to ensure allocation-free rendering.
 */
private class PrecomputedDot(
    val dotIndex: Int,
    val itemDate: LocalDate,
    val isPast: Boolean,
    val isCurrent: Boolean
) {
    var x: Float = 0f
    var y: Float = 0f
    var size: Float = 0f
    var baseColor: Int = 0
    var alpha: Int = FULL_ALPHA
    var cornerRadius: Float = 0f
    var centerX: Float = 0f
    var centerY: Float = 0f
    var radius: Float = 0f
    var shapePath: Path? = null
    var textLayout: StaticLayout? = null
    var textScaleFactor: Float = 1.0f
    var textTranslateX: Float = 0f
    var textTranslateY: Float = 0f
    var textCenterOffsetX: Float = 0f
    var textCenterOffsetY: Float = 0f
}

private fun formatHealthText(value: Float, metric: String): CharSequence {
    val (text, suffix) = when (metric) {
        "STEPS" -> {
            if (value >= THOUSAND_STEPS) {
                Pair(String.format(Locale.US, "%.1f", value / THOUSAND_STEPS.toFloat()), "k")
            } else {
                Pair(value.toInt().toString(), "")
            }
        }
        "CALORIES" -> Pair(value.toInt().toString(), "kcal")
        "DISTANCE" -> Pair(String.format(Locale.US, "%.1f", value), "km")
        "SLEEP" -> Pair(String.format(Locale.US, "%.1f", value), "h")
        else -> Pair(value.toInt().toString(), "")
    }

    if (suffix.isEmpty()) return text

    val spannable = SpannableString(text + suffix)
    spannable.setSpan(
        RelativeSizeSpan(RELATIVE_SIZE_SPAN_PROPORTION),
        text.length,
        spannable.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannable
}

/**
 * Renders the grid on a Canvas.
 * Optimized with strategy pattern for shape drawing and pre-allocated dot geometry for zero-allocation rendering.
 */
@Suppress("TooManyFunctions")
class CanvasRenderer(
    private var gridState: GridState,
    val colorScheme: ColorScheme,
    private val screenWidth: Int,
    private val screenHeight: Int,
    customTypeface: Typeface? = null
) {
    companion object {
        private const val DEFAULT_PADDING_SCALE = 0.05f
        private const val DEFAULT_HEALTH_GOAL = 10000f
        private const val MIN_UNIT_SCALE = 0.5f
        private const val MAX_UNIT_SCALE = 1.0f
        private const val TEXT_SIZE_SCALE = 0.40f
        private const val TEXT_OVERLAY_ALPHA = 200
        private const val BG_CACHE_INTERVAL_MS = 60_000L
        private const val MAX_SINGLE_LINE_WIDTH = 10000
        private const val PROGRESS_BASE_ALPHA_RATIO = 0.2f
    }

    private var gridConfig: GridConfig
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Style settings
    private var unitShape: UnitShape = UnitShape.DEFAULT
    private var unitScale: Float = 1.0f
    private var horizontalPaddingScale: Float = DEFAULT_PADDING_SCALE
    private var topPaddingScale: Float = DEFAULT_PADDING_SCALE
    private var bottomPaddingScale: Float = DEFAULT_PADDING_SCALE

    private var shapeDrawer: ShapeDrawer = RoundedSquareDrawer()

    // Precomputed dot collection for allocation-free render loop
    private val dots = ArrayList<PrecomputedDot>()

    // Cache background color to reduce allocations in render loop
    private var lastBgUpdateMillis: Long = 0L
    private var cachedHour: Int = -1
    private var cachedBgColor: Int = Color.BLACK

    // Health Connect formatting cache
    private var healthCache: Map<LocalDate, Float>? = null
    private var healthGoal: Float = DEFAULT_HEALTH_GOAL
    private var healthMetric: String = "NONE"
    private var showStatOverlay: Boolean = false
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = customTypeface ?: Typeface.DEFAULT_BOLD
    }

    init {
        gridConfig = calculateGrid()
        updateBackgroundCache(force = true)
        updateShapeDrawer()
        rebuildDots()
    }

    fun updateHealthData(
        metric: String,
        goal: Float,
        showOverlay: Boolean,
        cache: Map<LocalDate, Float>?
    ) {
        this.healthMetric = metric
        this.healthGoal = goal
        this.showStatOverlay = showOverlay
        this.healthCache = cache
        updateDotsHealth()
    }

    private fun calculateGrid(): GridConfig {
        return GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = horizontalPaddingScale,
            topMarginPercent = topPaddingScale,
            bottomMarginPercent = bottomPaddingScale
        )
    }

    fun render(canvas: Canvas, currentItemOpacity: Float) {
        updateBackgroundCache()
        canvas.drawColor(cachedBgColor)

        val fullAlpha = FULL_ALPHA
        val dotCount = dots.size
        for (i in 0 until dotCount) {
            val dot = dots[i]
            paint.color = dot.baseColor
            paint.alpha = if (dot.isCurrent) {
                (fullAlpha * currentItemOpacity).toInt()
            } else {
                dot.alpha
            }
            shapeDrawer.draw(canvas, dot, paint)

            val layout = dot.textLayout
            if (layout != null) {
                canvas.save()
                canvas.translate(dot.textTranslateX, dot.textTranslateY)
                if (dot.textScaleFactor < 1.0f) {
                    canvas.scale(dot.textScaleFactor, dot.textScaleFactor)
                }
                canvas.translate(dot.textCenterOffsetX, dot.textCenterOffsetY)
                layout.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun updateBackgroundCache(force: Boolean = false) {
        if (!force && !colorScheme.isDynamic) return
        val currentTime = System.currentTimeMillis()
        if (force || currentTime - lastBgUpdateMillis > BG_CACHE_INTERVAL_MS) {
            val hour = LocalDateTime.now().hour
            if (force || hour != cachedHour) {
                cachedHour = hour
                cachedBgColor = if (colorScheme.isDynamic) {
                    ColorUtils.adaptBackgroundForTimeOfDay(colorScheme.backgroundColor, hour)
                } else {
                    colorScheme.backgroundColor
                }
                textPaint.color = cachedBgColor
                textPaint.alpha = TEXT_OVERLAY_ALPHA
            }
            lastBgUpdateMillis = currentTime
        }
    }

    private fun rebuildDots() {
        dots.clear()
        val effectiveSize = gridConfig.dotSize * unitScale.coerceIn(MIN_UNIT_SCALE, MAX_UNIT_SCALE)
        val offset = (gridConfig.dotSize - effectiveSize) / 2f
        val startX = gridConfig.offsetX
        val startY = gridConfig.offsetY
        val cellSize = gridConfig.dotSize + gridConfig.spacing

        textPaint.textSize = effectiveSize * TEXT_SIZE_SCALE
        textPaint.color = cachedBgColor
        textPaint.alpha = TEXT_OVERLAY_ALPHA

        var dotIndex = 0
        for (row in 0 until gridConfig.rows) {
            for (col in 0 until gridConfig.columns) {
                if (dotIndex >= gridState.totalItems) break

                val x = startX + col * cellSize + offset
                val y = startY + row * cellSize + offset
                val dot = createPrecomputedDot(dotIndex, x, y, effectiveSize)

                if (showStatOverlay && healthMetric != "NONE" && healthCache != null) {
                    rebuildDotText(dot)
                }

                dots.add(dot)
                dotIndex++
            }
        }
    }

    private fun createPrecomputedDot(
        dotIndex: Int,
        x: Float,
        y: Float,
        effectiveSize: Float
    ): PrecomputedDot {
        val itemDate = gridState.startDate.plusDays(dotIndex.toLong())
        val isPast = dotIndex < gridState.pastItems
        val isCurrent = dotIndex == gridState.currentIndex
        val usesPrecomputedPath = usesPathDrawer(unitShape)

        val baseColor = when {
            isPast -> colorScheme.pastYearsColor
            isCurrent -> colorScheme.currentYearColor
            else -> colorScheme.futureYearsColor
        }

        val dot = PrecomputedDot(dotIndex, itemDate, isPast, isCurrent)
        dot.x = x
        dot.y = y
        dot.size = effectiveSize
        dot.baseColor = baseColor
        dot.alpha = computeInitialDotAlpha(isCurrent, isPast, baseColor, itemDate)
        dot.cornerRadius = effectiveSize * UnitShapeGeometry.ROUNDED_SQUARE_CORNER_RATIO
        dot.radius = effectiveSize / 2f
        dot.centerX = x + dot.radius
        dot.centerY = y + dot.radius
        dot.shapePath = if (usesPrecomputedPath) {
            UnitShapeGeometry.buildPath(unitShape, x, y, effectiveSize)
        } else {
            null
        }
        return dot
    }

    private fun usesPathDrawer(shape: UnitShape): Boolean {
        return shape != UnitShape.CIRCLE &&
            shape != UnitShape.ROUNDED_SQUARE &&
            shape != UnitShape.SQUARE
    }

    private fun computeInitialDotAlpha(
        isCurrent: Boolean,
        isPast: Boolean,
        baseColor: Int,
        itemDate: LocalDate
    ): Int {
        return when {
            isCurrent -> FULL_ALPHA
            isPast && healthMetric != "NONE" && healthCache != null -> {
                val value = healthCache?.get(itemDate) ?: 0f
                val progress = (value / healthGoal).coerceIn(0f, 1f)
                val originalAlpha = Color.alpha(baseColor)
                val baseAlpha = (originalAlpha * PROGRESS_BASE_ALPHA_RATIO).toInt()
                baseAlpha + ((originalAlpha - baseAlpha) * progress).toInt()
            }
            else -> Color.alpha(baseColor)
        }
    }

    private fun updateDotsHealth() {
        val count = dots.size
        for (i in 0 until count) {
            val dot = dots[i]
            if (dot.isPast && healthMetric != "NONE" && healthCache != null) {
                val value = healthCache?.get(dot.itemDate) ?: 0f
                val progress = (value / healthGoal).coerceIn(0f, 1f)
                val originalAlpha = Color.alpha(dot.baseColor)
                val baseAlpha = (originalAlpha * PROGRESS_BASE_ALPHA_RATIO).toInt()
                dot.alpha = baseAlpha + ((originalAlpha - baseAlpha) * progress).toInt()
            } else if (!dot.isCurrent) {
                dot.alpha = Color.alpha(dot.baseColor)
            }

            if (showStatOverlay && healthMetric != "NONE" && healthCache != null) {
                rebuildDotText(dot)
            } else {
                dot.textLayout = null
            }
        }
    }

    private fun rebuildDotText(dot: PrecomputedDot) {
        val value = if (dot.isCurrent || dot.isPast) {
            healthCache?.get(dot.itemDate)
        } else {
            null
        }

        if (value == null) {
            dot.textLayout = null
            return
        }

        val text = formatHealthText(value, healthMetric)
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, MAX_SINGLE_LINE_WIDTH)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        var actualTextWidth = 0f
        val lineCount = staticLayout.lineCount
        for (i in 0 until lineCount) {
            val lineWidth = staticLayout.getLineWidth(i)
            if (lineWidth > actualTextWidth) {
                actualTextWidth = lineWidth
            }
        }

        val actualTextHeight = staticLayout.height.toFloat()
        val safeBounds = dot.size * unitShape.textSafeBoundsScale
        val scaleX = if (actualTextWidth > safeBounds) safeBounds / actualTextWidth else 1.0f
        val scaleY = if (actualTextHeight > safeBounds) safeBounds / actualTextHeight else 1.0f

        dot.textScaleFactor = kotlin.math.min(scaleX, scaleY)
        dot.textTranslateX = dot.x + dot.size / 2f
        dot.textTranslateY = dot.y + dot.size / 2f
        dot.textCenterOffsetX = -actualTextWidth / 2f
        dot.textCenterOffsetY = -actualTextHeight / 2f
        dot.textLayout = staticLayout
    }

    fun updateGridState(newState: GridState) {
        gridState = newState
        gridConfig = calculateGrid()
        rebuildDots()
    }

    fun updateStyle(
        shapeId: String,
        scale: Float,
        horizontalPaddingScale: Float = DEFAULT_PADDING_SCALE,
        topPaddingScale: Float = DEFAULT_PADDING_SCALE,
        bottomPaddingScale: Float = DEFAULT_PADDING_SCALE
    ) {
        this.unitShape = UnitShape.fromId(shapeId)
        this.unitScale = scale
        this.horizontalPaddingScale = horizontalPaddingScale
        this.topPaddingScale = topPaddingScale
        this.bottomPaddingScale = bottomPaddingScale
        gridConfig = calculateGrid()
        updateShapeDrawer()
        rebuildDots()
    }

    private fun updateShapeDrawer() {
        shapeDrawer = when (unitShape) {
            UnitShape.CIRCLE -> CircleDrawer()
            UnitShape.ROUNDED_SQUARE -> RoundedSquareDrawer()
            UnitShape.SQUARE -> SquareDrawer()
            else -> PathShapeDrawer()
        }
    }

    private interface ShapeDrawer {
        fun draw(canvas: Canvas, dot: PrecomputedDot, paint: Paint)
    }

    private class CircleDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, dot: PrecomputedDot, paint: Paint) {
            canvas.drawCircle(dot.centerX, dot.centerY, dot.radius, paint)
        }
    }

    private class RoundedSquareDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, dot: PrecomputedDot, paint: Paint) {
            canvas.drawRoundRect(
                dot.x,
                dot.y,
                dot.x + dot.size,
                dot.y + dot.size,
                dot.cornerRadius,
                dot.cornerRadius,
                paint
            )
        }
    }

    private class SquareDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, dot: PrecomputedDot, paint: Paint) {
            canvas.drawRect(dot.x, dot.y, dot.x + dot.size, dot.y + dot.size, paint)
        }
    }

    private class PathShapeDrawer : ShapeDrawer {
        override fun draw(canvas: Canvas, dot: PrecomputedDot, paint: Paint) {
            dot.shapePath?.let { canvas.drawPath(it, paint) }
        }
    }
}
