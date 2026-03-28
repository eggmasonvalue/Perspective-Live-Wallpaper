package com.perspectivelive.wallpaper.data

import com.perspectivelive.wallpaper.service.HealthConnectManager

data class StyleConfig(
    val schemeId: String,
    val shapeId: String = "rounded_square",
    val scale: Float = 1.0f,
    val paddingScale: Float = 0.05f,
    val pulsePeriodMs: Long = 2000L,
    val healthMetric: String = HealthConnectManager.METRIC_NONE,
    val healthGoal: Float = 10000f,
    val showStatOverlay: Boolean = false
)
