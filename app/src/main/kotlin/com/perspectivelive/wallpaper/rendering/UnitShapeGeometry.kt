package com.perspectivelive.wallpaper.rendering

import android.graphics.Path
import android.graphics.RectF
import com.perspectivelive.wallpaper.data.UnitShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Single source of truth for constructing unit shape geometry shared by
 * [CanvasRenderer], live wallpaper previews, and Craft shape tiles.
 */
object UnitShapeGeometry {

    const val ROUNDED_SQUARE_CORNER_RATIO = 0.16f

    private const val TWO_PI = 2.0 * PI
    private const val HALF_PI = PI / 2.0
    private const val SQUIRCLE_SAMPLES = 64
    private const val SQUIRCLE_EXPONENT = 3.2
    private const val SQUIRCLE_POWER = 2.0 / SQUIRCLE_EXPONENT
    private const val RHOMBUS_CORNER_FRACTION = 0.12f
    private const val HEXAGON_SIDES = 6
    private const val HEXAGON_CORNER_FRACTION = 0.18f
    private const val OCTAGON_CHAMFER_RATIO = 0.28f
    private const val OCTAGON_CORNER_FRACTION = 0.10f
    private const val PILL_WIDTH_RATIO = 0.56f
    private const val CLOVER_SAMPLES = 72
    private const val CLOVER_LOBES = 4
    private const val CLOVER_BASE_RADIUS = 0.80
    private const val CLOVER_AMPLITUDE = 0.20
    private const val SCALLOP_SAMPLES = 96
    private const val SCALLOP_LOBES = 10
    private const val SCALLOP_BASE_RADIUS = 0.91
    private const val SCALLOP_AMPLITUDE = 0.09
    private const val LEAF_LARGE_CORNER_RATIO = 0.48f
    private const val LEAF_SMALL_CORNER_RATIO = 0.10f
    private const val SPARK_POINTS = 4
    private const val SPARK_INNER_RATIO = 0.44f
    private const val SPARK_CORNER_FRACTION = 0.28f
    private const val MIN_EDGE_LENGTH = 1e-4f
    private const val MIN_EXTENT = 1e-6
    private const val MIN_POLYGON_VERTICES = 3

    /**
     * Builds a closed [Path] for [shape] inscribed within `[x, x + size] x [y, y + size]`.
     */
    fun buildPath(shape: UnitShape, x: Float, y: Float, size: Float): Path {
        val path = Path()
        if (size <= 0f) return path

        val half = size / 2f
        val cx = x + half
        val cy = y + half

        when (shape) {
            UnitShape.CIRCLE -> path.addCircle(cx, cy, half, Path.Direction.CW)
            UnitShape.ROUNDED_SQUARE -> {
                val radius = size * ROUNDED_SQUARE_CORNER_RATIO
                path.addRoundRect(x, y, x + size, y + size, radius, radius, Path.Direction.CW)
            }
            UnitShape.SQUIRCLE -> addClosedPolyline(path, sampleSquirclePoints(x, y, size))
            UnitShape.SQUARE -> path.addRect(x, y, x + size, y + size, Path.Direction.CW)
            UnitShape.RHOMBUS -> addRoundedPolygon(
                path,
                computeRhombusVertices(x, y, size),
                RHOMBUS_CORNER_FRACTION
            )
            UnitShape.HEXAGON -> addRoundedPolygon(
                path,
                computeHexagonVertices(x, y, size),
                HEXAGON_CORNER_FRACTION
            )
            UnitShape.OCTAGON -> addRoundedPolygon(
                path,
                computeOctagonVertices(x, y, size),
                OCTAGON_CORNER_FRACTION
            )
            UnitShape.PILL -> {
                val pillWidth = size * PILL_WIDTH_RATIO
                val left = cx - pillWidth / 2f
                val radius = pillWidth / 2f
                path.addRoundRect(left, y, left + pillWidth, y + size, radius, radius, Path.Direction.CW)
            }
            UnitShape.CLOVER -> addClosedPolyline(path, sampleCloverPoints(x, y, size))
            UnitShape.SCALLOP -> addClosedPolyline(path, sampleScallopPoints(x, y, size))
            UnitShape.LEAF -> {
                val big = size * LEAF_LARGE_CORNER_RATIO
                val small = size * LEAF_SMALL_CORNER_RATIO
                val radii = floatArrayOf(big, big, small, small, big, big, small, small)
                path.addRoundRect(RectF(x, y, x + size, y + size), radii, Path.Direction.CW)
            }
            UnitShape.SPARK -> addRoundedPolygon(
                path,
                computeSparkVertices(x, y, size),
                SPARK_CORNER_FRACTION
            )
        }
        return path
    }

    internal fun sampleSquirclePoints(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        return List(SQUIRCLE_SAMPLES) { index ->
            val angle = TWO_PI * index / SQUIRCLE_SAMPLES
            val cosA = cos(angle)
            val sinA = sin(angle)
            val px = cx + (sign(cosA) * abs(cosA).pow(SQUIRCLE_POWER) * half).toFloat()
            val py = cy + (sign(sinA) * abs(sinA).pow(SQUIRCLE_POWER) * half).toFloat()
            px to py
        }
    }

    internal fun computeRhombusVertices(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        return listOf(
            cx to y,
            (x + size) to cy,
            cx to (y + size),
            x to cy
        )
    }

    internal fun computeHexagonVertices(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        return List(HEXAGON_SIDES) { index ->
            val angle = -HALF_PI + TWO_PI * index / HEXAGON_SIDES
            (cx + (cos(angle) * half).toFloat()) to (cy + (sin(angle) * half).toFloat())
        }
    }

    internal fun computeOctagonVertices(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val cut = size * OCTAGON_CHAMFER_RATIO
        val right = x + size
        val bottom = y + size
        return listOf(
            (x + cut) to y,
            (right - cut) to y,
            right to (y + cut),
            right to (bottom - cut),
            (right - cut) to bottom,
            (x + cut) to bottom,
            x to (bottom - cut),
            x to (y + cut)
        )
    }

    internal fun sampleCloverPoints(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        val raw = List(CLOVER_SAMPLES) { index ->
            val angle = TWO_PI * index / CLOVER_SAMPLES
            val r = CLOVER_BASE_RADIUS - CLOVER_AMPLITUDE * cos(CLOVER_LOBES * angle)
            (r * cos(angle)) to (r * sin(angle))
        }
        val maxExtent = raw.maxOf { (rx, ry) -> maxOf(abs(rx), abs(ry)) }.coerceAtLeast(MIN_EXTENT)
        val scale = half / maxExtent
        return raw.map { (rx, ry) ->
            (cx + (rx * scale).toFloat()) to (cy + (ry * scale).toFloat())
        }
    }

    internal fun sampleScallopPoints(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        return List(SCALLOP_SAMPLES) { index ->
            val angle = TWO_PI * index / SCALLOP_SAMPLES
            val r = (SCALLOP_BASE_RADIUS + SCALLOP_AMPLITUDE * cos(SCALLOP_LOBES * angle)) * half
            (cx + (r * cos(angle)).toFloat()) to (cy + (r * sin(angle)).toFloat())
        }
    }

    internal fun computeSparkVertices(x: Float, y: Float, size: Float): List<Pair<Float, Float>> {
        val half = size / 2f
        val cx = x + half
        val cy = y + half
        val totalVertices = SPARK_POINTS * 2
        val innerRadius = half * SPARK_INNER_RATIO
        return List(totalVertices) { index ->
            val angle = -HALF_PI + TWO_PI * index / totalVertices
            val radius = if (index % 2 == 0) half else innerRadius
            (cx + (cos(angle) * radius).toFloat()) to (cy + (sin(angle) * radius).toFloat())
        }
    }

    private fun addClosedPolyline(path: Path, points: List<Pair<Float, Float>>) {
        if (points.isEmpty()) return
        val first = points[0]
        path.moveTo(first.first, first.second)
        for (i in 1 until points.size) {
            val pt = points[i]
            path.lineTo(pt.first, pt.second)
        }
        path.close()
    }

    private fun addRoundedPolygon(
        path: Path,
        vertices: List<Pair<Float, Float>>,
        cornerFraction: Float
    ) {
        val count = vertices.size
        if (count < MIN_POLYGON_VERTICES) return

        for (i in 0 until count) {
            val prev = vertices[(i - 1 + count) % count]
            val curr = vertices[i]
            val next = vertices[(i + 1) % count]

            val dxPrev = prev.first - curr.first
            val dyPrev = prev.second - curr.second
            val lenPrev = hypot(dxPrev.toDouble(), dyPrev.toDouble()).toFloat()

            val dxNext = next.first - curr.first
            val dyNext = next.second - curr.second
            val lenNext = hypot(dxNext.toDouble(), dyNext.toDouble()).toFloat()

            if (lenPrev < MIN_EDGE_LENGTH || lenNext < MIN_EDGE_LENGTH) continue

            val trim = min(lenPrev, lenNext) * cornerFraction
            val startX = curr.first + (dxPrev / lenPrev) * trim
            val startY = curr.second + (dyPrev / lenPrev) * trim
            val endX = curr.first + (dxNext / lenNext) * trim
            val endY = curr.second + (dyNext / lenNext) * trim

            if (i == 0) {
                path.moveTo(startX, startY)
            } else {
                path.lineTo(startX, startY)
            }
            path.quadTo(curr.first, curr.second, endX, endY)
        }
        path.close()
    }
}
