package com.perspectivelive.wallpaper.rendering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GridCalculator to verify that horizontal, top, and bottom margin settings
 * independently control usable bounds, vertical placement, and grid reflow.
 */
class GridCalculatorTest {

    @Test
    fun `calculateGridLayout changes dot size and offsets based on margins`() {
        val totalDots = 100
        val screenWidth = 1000
        val screenHeight = 2000
        val defaultMargin = 0.05f
        val largeMargin = 0.20f

        val configDefault = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = defaultMargin,
            topMarginPercent = defaultMargin,
            bottomMarginPercent = defaultMargin
        )

        val configLarge = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = largeMargin,
            topMarginPercent = largeMargin,
            bottomMarginPercent = largeMargin
        )

        assertNotEquals("OffsetX should be different", configDefault.offsetX, configLarge.offsetX, 0.01f)
        assertNotEquals("OffsetY should be different", configDefault.offsetY, configLarge.offsetY, 0.01f)
        assertTrue("Dot size should decrease with larger margin", configLarge.dotSize < configDefault.dotSize)
    }

    @Test
    fun `calculateGridLayout respects zero margin`() {
        val totalDots = 100
        val screenWidth = 1000
        val screenHeight = 2000

        val configZero = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.0f,
            topMarginPercent = 0.0f,
            bottomMarginPercent = 0.0f
        )

        val configMargin = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            topMarginPercent = 0.05f,
            bottomMarginPercent = 0.05f
        )

        assertTrue("Zero margin should result in larger dots", configZero.dotSize > configMargin.dotSize)
    }

    @Test
    fun `increasing vertical margins reflows grid to more columns and fewer rows`() {
        val totalDots = 90
        val screenWidth = 1080
        val screenHeight = 2400

        val tallConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            topMarginPercent = 0.05f,
            bottomMarginPercent = 0.05f
        )

        val shortUsableConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            topMarginPercent = 0.30f,
            bottomMarginPercent = 0.30f
        )

        assertTrue(
            "Larger vertical margins should increase columns (${shortUsableConfig.columns} vs ${tallConfig.columns})",
            shortUsableConfig.columns > tallConfig.columns
        )
        assertTrue(
            "Larger vertical margins should decrease rows (${shortUsableConfig.rows} vs ${tallConfig.rows})",
            shortUsableConfig.rows < tallConfig.rows
        )
        assertTrue(
            "Vertical offset should respect at least 30% top margin",
            shortUsableConfig.offsetY >= screenHeight * 0.30f - 1f
        )
    }

    @Test
    fun `increasing horizontal margin reflows grid to fewer columns and more rows`() {
        val totalDots = 90
        val screenWidth = 1080
        val screenHeight = 2400

        val wideConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.0f,
            topMarginPercent = 0.25f,
            bottomMarginPercent = 0.25f
        )

        val narrowConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.20f,
            topMarginPercent = 0.25f,
            bottomMarginPercent = 0.25f
        )

        assertTrue(
            "Larger horizontal margin should decrease columns (${narrowConfig.columns} vs ${wideConfig.columns})",
            narrowConfig.columns < wideConfig.columns
        )
        assertTrue(
            "Horizontal offset should respect at least 20% horizontal margin",
            narrowConfig.offsetX >= screenWidth * 0.20f - 1f
        )
    }

    @Test
    fun `asymmetric top and bottom margins shift grid into the usable vertical band`() {
        val totalDots = 90
        val screenWidth = 1080
        val screenHeight = 2400

        val topHeavy = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            topMarginPercent = 0.35f,
            bottomMarginPercent = 0.05f
        )

        val bottomHeavy = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            topMarginPercent = 0.05f,
            bottomMarginPercent = 0.35f
        )

        assertEquals(
            "Same total usable height should produce identical dotSize",
            topHeavy.dotSize,
            bottomHeavy.dotSize,
            0.01f
        )
        assertEquals("Same total usable height should produce identical columns", topHeavy.columns, bottomHeavy.columns)
        assertEquals("Same total usable height should produce identical rows", topHeavy.rows, bottomHeavy.rows)

        val gridHeight = (topHeavy.rows * topHeavy.dotSize) + ((topHeavy.rows - 1) * topHeavy.spacing)
        assertTrue(
            "Top-heavy config should sit lower than bottom-heavy config",
            topHeavy.offsetY > bottomHeavy.offsetY
        )
        assertTrue(
            "Top-heavy config should clear 35% top margin",
            topHeavy.offsetY >= screenHeight * 0.35f - 1f
        )
        assertTrue(
            "Bottom-heavy config should clear 35% bottom margin",
            bottomHeavy.offsetY + gridHeight <= screenHeight * 0.65f + 1f
        )
    }
}
