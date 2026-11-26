package com.perspectivelive.wallpaper.settings.ui

import com.perspectivelive.wallpaper.service.HealthConnectManager

enum class WallpaperMode {
    MACRO,
    MICRO
}

enum class StudioTab {
    HORIZON,
    PALETTE,
    CRAFT
}

enum class SurfacePeekMode {
    CLEAN,
    LOCK,
    HOME
}

enum class CustomColorSlot {
    BACKGROUND,
    PAST_FUTURE,
    CURRENT
}

data class HealthMetricSpec(
    val id: String,
    val label: String,
    val unit: String,
    val defaultGoal: Float,
    val step: Float
)

object HealthMetricCatalog {
    val all: List<HealthMetricSpec> = listOf(
        HealthMetricSpec(
            id = HealthConnectManager.METRIC_NONE,
            label = "Off",
            unit = "",
            defaultGoal = 0f,
            step = 0f
        ),
        HealthMetricSpec(
            id = HealthConnectManager.METRIC_STEPS,
            label = "Steps",
            unit = "steps",
            defaultGoal = 10_000f,
            step = 1_000f
        ),
        HealthMetricSpec(
            id = HealthConnectManager.METRIC_CALORIES,
            label = "Cals",
            unit = "kcal",
            defaultGoal = 2_200f,
            step = 100f
        ),
        HealthMetricSpec(
            id = HealthConnectManager.METRIC_DISTANCE,
            label = "Dist",
            unit = "km",
            defaultGoal = 7f,
            step = 1f
        ),
        HealthMetricSpec(
            id = HealthConnectManager.METRIC_SLEEP,
            label = "Sleep",
            unit = "hrs",
            defaultGoal = 8f,
            step = 0.5f
        )
    )

    fun find(id: String): HealthMetricSpec = all.firstOrNull { it.id == id } ?: all.first()
}
