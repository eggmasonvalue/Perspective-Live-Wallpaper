package com.perspectivelive.wallpaper.rendering

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GridCalculator to verify that horizontal and vertical margin settings
 * independently control usable bounds and grid reflow.
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
            verticalMarginPercent = defaultMargin
        )

        val configLarge = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = largeMargin,
            verticalMarginPercent = largeMargin
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
            verticalMarginPercent = 0.0f
        )

        val configMargin = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            verticalMarginPercent = 0.05f
        )

        assertTrue("Zero margin should result in larger dots", configZero.dotSize > configMargin.dotSize)
    }

    @Test
    fun `increasing vertical margin reflows grid to more columns and fewer rows`() {
        val totalDots = 90
        val screenWidth = 1080
        val screenHeight = 2400

        val tallConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            verticalMarginPercent = 0.05f
        )

        val shortUsableConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.05f,
            verticalMarginPercent = 0.30f
        )

        assertTrue(
            "Larger vertical margin should increase columns (${shortUsableConfig.columns} vs ${tallConfig.columns})",
            shortUsableConfig.columns > tallConfig.columns
        )
        assertTrue(
            "Larger vertical margin should decrease rows (${shortUsableConfig.rows} vs ${tallConfig.rows})",
            shortUsableConfig.rows < tallConfig.rows
        )
        assertTrue(
            "Vertical offset should respect at least 30% vertical margin",
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
            verticalMarginPercent = 0.25f
        )

        val narrowConfig = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizontalMarginPercent = 0.20f,
            verticalMarginPercent = 0.25f
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
}
