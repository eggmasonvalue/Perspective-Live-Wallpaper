package com.perspectivelive.wallpaper.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitShapeGeometryTest {

    companion object {
        private const val ORIGIN_X = 24f
        private const val ORIGIN_Y = 48f
        private const val SIZE = 100f
        private const val BOUNDS_EPSILON = 0.01f
        private const val CENTER_EPSILON = 0.5f
    }

    @Test
    fun squirclePointsStayWithinBoundsAndAreCentered() {
        val points = UnitShapeGeometry.sampleSquirclePoints(ORIGIN_X, ORIGIN_Y, SIZE)
        assertTrue(points.size >= 32)
        assertWithinBoundsAndCentered(points)
    }

    @Test
    fun rhombusVerticesStayWithinBoundsAndAreCentered() {
        val vertices = UnitShapeGeometry.computeRhombusVertices(ORIGIN_X, ORIGIN_Y, SIZE)
        assertEquals(4, vertices.size)
        assertWithinBoundsAndCentered(vertices)
    }

    @Test
    fun hexagonVerticesStayWithinBoundsAndAreCentered() {
        val vertices = UnitShapeGeometry.computeHexagonVertices(ORIGIN_X, ORIGIN_Y, SIZE)
        assertEquals(6, vertices.size)
        assertWithinBoundsAndCentered(vertices)
    }

    @Test
    fun octagonVerticesStayWithinBoundsAndAreCentered() {
        val vertices = UnitShapeGeometry.computeOctagonVertices(ORIGIN_X, ORIGIN_Y, SIZE)
        assertEquals(8, vertices.size)
        assertWithinBoundsAndCentered(vertices)
    }

    @Test
    fun cloverPointsFillBoundsSymmetrically() {
        val points = UnitShapeGeometry.sampleCloverPoints(ORIGIN_X, ORIGIN_Y, SIZE)
        assertTrue(points.size >= 64)
        assertWithinBoundsAndCentered(points)
        val maxX = points.maxOf { it.first }
        assertEquals(ORIGIN_X + SIZE, maxX, 0.25f)
    }

    @Test
    fun scallopPointsStayWithinBoundsAndAreCentered() {
        val points = UnitShapeGeometry.sampleScallopPoints(ORIGIN_X, ORIGIN_Y, SIZE)
        assertTrue(points.size >= 64)
        assertWithinBoundsAndCentered(points)
    }

    @Test
    fun sparkVerticesStayWithinBoundsAndAreCentered() {
        val vertices = UnitShapeGeometry.computeSparkVertices(ORIGIN_X, ORIGIN_Y, SIZE)
        assertEquals(8, vertices.size)
        assertWithinBoundsAndCentered(vertices)
    }

    private fun assertWithinBoundsAndCentered(points: List<Pair<Float, Float>>) {
        val minX = ORIGIN_X - BOUNDS_EPSILON
        val maxX = ORIGIN_X + SIZE + BOUNDS_EPSILON
        val minY = ORIGIN_Y - BOUNDS_EPSILON
        val maxY = ORIGIN_Y + SIZE + BOUNDS_EPSILON

        for ((px, py) in points) {
            assertTrue("x=$px outside [$minX, $maxX]", px in minX..maxX)
            assertTrue("y=$py outside [$minY, $maxY]", py in minY..maxY)
        }

        val avgX = points.map { it.first }.average().toFloat()
        val avgY = points.map { it.second }.average().toFloat()
        assertEquals(ORIGIN_X + SIZE / 2f, avgX, CENTER_EPSILON)
        assertEquals(ORIGIN_Y + SIZE / 2f, avgY, CENTER_EPSILON)
    }
}
