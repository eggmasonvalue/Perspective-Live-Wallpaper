package com.perspectivelive.wallpaper.rendering

import android.graphics.Canvas
import java.time.LocalDate

data class RenderItemParams(
    val canvas: Canvas,
    val dotIndex: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val itemDate: LocalDate,
    val currentItemOpacity: Float
)
