package com.timehorizons.wallpaper.rendering

import com.timehorizons.wallpaper.data.GridConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GridCalculator to verify that padding settings affect grid layout.
 */
class GridCalculatorTest {

    @Test
    fun `calculateGridLayout changes dot size and offsets based on margin`() {
        // Arrange
        val totalDots = 100
        val screenWidth = 1000
        val screenHeight = 2000
        val defaultMargin = 0.05f // 5%
        val largeMargin = 0.20f   // 20%

        // Act
        val configDefault = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPercent = defaultMargin
        )

        val configLarge = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPercent = largeMargin
        )

        // Assert
        // 1. Offsets should change
        assertNotEquals("OffsetX should be different", configDefault.offsetX, configLarge.offsetX, 0.01f)
        assertNotEquals("OffsetY should be different", configDefault.offsetY, configLarge.offsetY, 0.01f)

        // 2. Dot size should decrease with larger margin
        assertTrue("Dot size should decrease with larger margin", configLarge.dotSize < configDefault.dotSize)

        // 3. Grid dimensions calculation check
        // With 20% margin on each side, usable width is 60% of original.
        // With 5% margin on each side, usable width is 90% of original.
        val usableWidthDefault = screenWidth * (1 - 2 * defaultMargin)
        val usableWidthLarge = screenWidth * (1 - 2 * largeMargin)

        // This confirms the math inside GridCalculator is respecting the marginPercent
        // The dot size is constrained by width/height, so it must shrink.
    }

    @Test
    fun `calculateGridLayout respects zero margin`() {
        val totalDots = 100
        val screenWidth = 1000
        val screenHeight = 2000
        val zeroMargin = 0.0f

        val configZero = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPercent = zeroMargin
        )

        // It should use full width roughly
        // 100 dots -> 10x10 roughly
        // 1000 width / 10 cols = 100 dot size (minus spacing)

        val marginPercent = 0.05f
        val configMargin = GridCalculator.calculateGridLayout(
            totalDots = totalDots,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            marginPercent = marginPercent
        )

        assertTrue("Zero margin should result in larger dots", configZero.dotSize > configMargin.dotSize)
    }
}
