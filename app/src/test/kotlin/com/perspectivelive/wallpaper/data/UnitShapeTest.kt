package com.perspectivelive.wallpaper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitShapeTest {

    @Test
    fun allShapesHaveUniqueIdsAndLabels() {
        val ids = UnitShape.entries.map { it.id }
        val labels = UnitShape.entries.map { it.label }

        assertEquals(UnitShape.entries.size, ids.toSet().size)
        assertEquals(UnitShape.entries.size, labels.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
        assertTrue(labels.all { it.isNotBlank() })
    }

    @Test
    fun fromIdRoundTripsAllShapes() {
        for (shape in UnitShape.entries) {
            assertEquals(shape, UnitShape.fromId(shape.id))
        }
    }

    @Test
    fun fromIdFallsBackToDefaultForUnknownId() {
        assertEquals(UnitShape.DEFAULT, UnitShape.fromId("unknown_shape"))
        assertEquals(UnitShape.DEFAULT, UnitShape.fromId(""))
    }

    @Test
    fun textSafeBoundsScaleIsWithinValidRange() {
        for (shape in UnitShape.entries) {
            assertTrue(
                "${shape.id} textSafeBoundsScale out of range: ${shape.textSafeBoundsScale}",
                shape.textSafeBoundsScale > 0f && shape.textSafeBoundsScale <= 1f
            )
        }
    }
}
