package com.perspectivelive.wallpaper.data

/**
 * Catalog of geometric and expressive unit shapes rendered across the timeline grid.
 *
 * @property id Stable persistence identifier stored in SharedPreferences.
 * @property label Concise user-facing display name.
 * @property textSafeBoundsScale Fraction of unit size available for centered stat overlay text.
 */
enum class UnitShape(
    val id: String,
    val label: String,
    val textSafeBoundsScale: Float
) {
    CIRCLE(id = "circle", label = "Circle", textSafeBoundsScale = 0.707f),
    ROUNDED_SQUARE(id = "rounded_square", label = "Rounded", textSafeBoundsScale = 0.82f),
    SQUIRCLE(id = "squircle", label = "Squircle", textSafeBoundsScale = 0.78f),
    SQUARE(id = "square", label = "Square", textSafeBoundsScale = 0.86f),
    RHOMBUS(id = "rhombus", label = "Rhombus", textSafeBoundsScale = 0.56f),
    HEXAGON(id = "hexagon", label = "Hexagon", textSafeBoundsScale = 0.72f),
    OCTAGON(id = "octagon", label = "Octagon", textSafeBoundsScale = 0.78f),
    PILL(id = "pill", label = "Capsule", textSafeBoundsScale = 0.50f),
    CLOVER(id = "clover", label = "Clover", textSafeBoundsScale = 0.62f),
    SCALLOP(id = "scallop", label = "Scallop", textSafeBoundsScale = 0.66f),
    LEAF(id = "leaf", label = "Leaf", textSafeBoundsScale = 0.74f),
    SPARK(id = "spark", label = "Spark", textSafeBoundsScale = 0.48f);

    companion object {
        val DEFAULT: UnitShape = ROUNDED_SQUARE

        private val byId: Map<String, UnitShape> = entries.associateBy { it.id }

        fun fromId(id: String): UnitShape = byId[id] ?: DEFAULT
    }
}
