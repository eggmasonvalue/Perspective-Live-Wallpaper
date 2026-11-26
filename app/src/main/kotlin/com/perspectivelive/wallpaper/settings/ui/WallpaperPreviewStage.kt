package com.perspectivelive.wallpaper.settings.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UnitShape
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.rendering.GridCalculator
import com.perspectivelive.wallpaper.rendering.UnitShapeGeometry
import com.perspectivelive.wallpaper.service.HealthConnectManager
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val DEFAULT_PHONE_ASPECT_RATIO = 9f / 20f
private const val MIN_ASPECT_RATIO = 0.38f
private const val MAX_ASPECT_RATIO = 0.65f
private const val STAGE_HEIGHT_DP = 240
private const val STAGE_PADDING_DP = 12
private const val STAGE_CONTENT_GAP_DP = 16
private const val STAGE_CONTROL_HEIGHT_DP = 40
private const val STAGE_CONTROL_GAP_DP = 10
private const val NEUTRAL_MAT_DARK = 0xFF1A1A1A
private const val NEUTRAL_MAT_LIGHT = 0xFFEFEFEF
private const val MINI_PREVIEW_CORNER_DP = 16
private const val STAGE_CORNER_DP = 28
private const val PULSE_MIN_ALPHA = 0.25f
private const val PULSE_ALPHA_SPAN = 0.75f
private const val NANOS_PER_MS = 1_000_000.0
private const val FULL_CIRCLE_RADIANS = 2.0 * PI
private const val HALF_PI = PI / 2.0
private const val FUTURE_DOT_ALPHA = 0.28f
private const val HEALTH_BASE_ALPHA = 0.22f
private const val HEALTH_ALPHA_SPAN = 0.78f
private const val HEALTH_TEXT_SCALE = 0.36f
private const val MIN_TEXT_DOT_PX = 24f
private const val THOUSAND = 1000f
private const val REF_SCREEN_WIDTH = 1080
private const val REF_SCREEN_HEIGHT = 2400

private val SAMPLE_STEPS = floatArrayOf(
    8420f, 11200f, 6350f, 9800f, 12450f, 7100f, 10300f, 5400f, 9100f, 10800f, 8950f, 13100f
)
private val SAMPLE_CALS = floatArrayOf(
    1950f, 2420f, 1780f, 2210f, 2650f, 1890f, 2340f, 1620f, 2100f, 2480f, 2050f, 2710f
)
private val SAMPLE_DIST = floatArrayOf(
    5.4f, 8.2f, 4.1f, 6.8f, 9.6f, 4.9f, 7.5f, 3.2f, 6.3f, 8.0f, 6.1f, 10.2f
)
private val SAMPLE_SLEEP = floatArrayOf(
    7.4f, 8.1f, 6.2f, 7.8f, 8.4f, 6.9f, 7.6f, 5.8f, 7.2f, 8.0f, 7.5f, 8.6f
)

@Composable
fun rememberDeviceAspectRatio(): Float {
    val context = LocalContext.current
    val containerSize = LocalWindowInfo.current.containerSize
    return remember(context, containerSize) {
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds
        val width = bounds?.width()?.takeIf { it > 0 } ?: containerSize.width
        val height = bounds?.height()?.takeIf { it > 0 } ?: containerSize.height
        if (width > 0 && height > 0) {
            val shortSide = min(width, height).toFloat()
            val longSide = maxOf(width, height).toFloat()
            (shortSide / longSide).coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
        } else {
            DEFAULT_PHONE_ASPECT_RATIO
        }
    }
}

@Composable
fun rememberBreathingAlpha(pulsePeriodMs: Long): Float {
    var alpha by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(pulsePeriodMs) {
        val period = pulsePeriodMs.coerceAtLeast(1L).toDouble()
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val elapsedMs = (now - startNanos) / NANOS_PER_MS
                val phase = (elapsedMs % period) / period
                val wave = 0.5 + 0.5 * sin(phase * FULL_CIRCLE_RADIANS - HALF_PI)
                alpha = (PULSE_MIN_ALPHA + PULSE_ALPHA_SPAN * wave).toFloat()
            }
        }
    }
    return alpha
}

/**
 * Pinned true-aspect live wallpaper stage on a color-neutral mat to preserve true palette perception.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WallpaperPreviewStage(
    prefs: UserPreferences,
    mode: WallpaperMode,
    gridState: GridState,
    colorScheme: ColorScheme,
    themeDisplayName: String,
    previewDark: Boolean,
    pulseAlpha: Float,
    onPreviewDarkChange: (Boolean) -> Unit,
    onExpandFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceAspect = rememberDeviceAspectRatio()
    val systemDark = isSystemInDarkTheme()
    val neutralMatColor = Color(if (systemDark) NEUTRAL_MAT_DARK else NEUTRAL_MAT_LIGHT)
    val refWidth = REF_SCREEN_WIDTH
    val refHeight = (refWidth / deviceAspect).toInt().coerceAtLeast(REF_SCREEN_HEIGHT / 2)
    val gridConfig = remember(
        gridState.totalItems,
        prefs.horizontalPaddingScale,
        prefs.topPaddingScale,
        prefs.bottomPaddingScale,
        refWidth,
        refHeight
    ) {
        GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = refWidth,
            screenHeight = refHeight,
            horizontalMarginPercent = prefs.horizontalPaddingScale,
            topMarginPercent = prefs.topPaddingScale,
            bottomMarginPercent = prefs.bottomPaddingScale
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(STAGE_CORNER_DP.dp),
        color = neutralMatColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(STAGE_HEIGHT_DP.dp)
                .padding(STAGE_PADDING_DP.dp),
            horizontalArrangement = Arrangement.spacedBy(STAGE_CONTENT_GAP_DP.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Exact device-aspect-ratio live wallpaper preview (tap to expand 1:1)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(deviceAspect)
                    .clip(RoundedCornerShape(MINI_PREVIEW_CORNER_DP.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(MINI_PREVIEW_CORNER_DP.dp)
                    )
                    .clickable(onClick = onExpandFullscreen)
            ) {
                LiveWallpaperCanvas(
                    prefs = prefs,
                    mode = mode,
                    gridState = gridState,
                    colorScheme = colorScheme,
                    pulseAlpha = pulseAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right: Uncluttered Stage Control Rail
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 4.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = themeDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dotLabel = if (gridState.totalItems == 1) "dot" else "dots"
                    Text(
                        text = "${gridConfig.columns} × ${gridConfig.rows} · ${gridState.totalItems} $dotLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                // Bottom-anchored action cluster with matched 40dp control heights and flush bottom alignment
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides STAGE_CONTROL_HEIGHT_DP.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(STAGE_CONTROL_GAP_DP.dp)) {
                        PairedVariantSelector(
                            previewDark = previewDark,
                            onPreviewDarkChange = onPreviewDarkChange
                        )

                        FilledTonalButton(
                            onClick = onExpandFullscreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = STAGE_CONTROL_HEIGHT_DP.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Full Screen 1:1",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PairedVariantSelector(
    previewDark: Boolean,
    onPreviewDarkChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ToggleButton(
            checked = previewDark,
            onCheckedChange = { if (it) onPreviewDarkChange(true) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = STAGE_CONTROL_HEIGHT_DP.dp)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(
                text = "Dark",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
        ToggleButton(
            checked = !previewDark,
            onCheckedChange = { if (it) onPreviewDarkChange(false) },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = STAGE_CONTROL_HEIGHT_DP.dp)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(
                text = "Light",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FullscreenWallpaperPreview(
    visible: Boolean,
    prefs: UserPreferences,
    mode: WallpaperMode,
    gridState: GridState,
    colorScheme: ColorScheme,
    pulseAlpha: Float,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiveWallpaperCanvas(
                prefs = prefs,
                mode = mode,
                gridState = gridState,
                colorScheme = colorScheme,
                pulseAlpha = pulseAlpha,
                modifier = Modifier.fillMaxSize()
            )

            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
        }
    }
}

@Composable
fun LiveWallpaperCanvas(
    prefs: UserPreferences,
    mode: WallpaperMode,
    gridState: GridState,
    colorScheme: ColorScheme,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geistTypeface = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.geist_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    val textPaint = remember(geistTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = geistTypeface
            textAlign = Paint.Align.CENTER
        }
    }

    val bgColor = Color(colorScheme.backgroundColor)
    val pastColor = Color(colorScheme.pastYearsColor)
    val currentColor = Color(colorScheme.currentYearColor)
    val unitShape = UnitShape.fromId(prefs.unitShapeId)
    val useHealth = mode == WallpaperMode.MICRO &&
        prefs.healthMetric != HealthConnectManager.METRIC_NONE &&
        prefs.dayCounterMode != DayCounterMode.THIS_YEAR_WEEKS

    Canvas(modifier = modifier) {
        drawRect(color = bgColor)

        val grid = GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = size.width.toInt(),
            screenHeight = size.height.toInt(),
            horizontalMarginPercent = prefs.horizontalPaddingScale,
            topMarginPercent = prefs.topPaddingScale,
            bottomMarginPercent = prefs.bottomPaddingScale
        )

        val effectiveSize = grid.dotSize * prefs.unitScale
        val innerOffset = (grid.dotSize - effectiveSize) / 2f
        val cellStep = grid.dotSize + grid.spacing
        val showOverlay = useHealth && prefs.showStatOverlay && effectiveSize >= MIN_TEXT_DOT_PX
        val unitPath = UnitShapeGeometry.buildPath(
            shape = unitShape,
            x = 0f,
            y = 0f,
            size = effectiveSize
        ).asComposePath()

        for (index in 0 until gridState.totalItems) {
            val row = index / grid.columns
            val col = index % grid.columns
            val x = grid.offsetX + col * cellStep + innerOffset
            val y = grid.offsetY + row * cellStep + innerOffset
            val healthVal = if (useHealth) sampleHealthValue(prefs.healthMetric, index) else null

            val dotColor = resolveDotColor(
                index = index,
                currentIndex = gridState.currentIndex,
                pastColor = pastColor,
                currentColor = currentColor,
                pulseAlpha = pulseAlpha,
                healthVal = healthVal,
                healthGoal = prefs.healthMetricGoal
            )

            translate(left = x, top = y) {
                drawPath(path = unitPath, color = dotColor)
            }

            if (showOverlay && healthVal != null && index <= gridState.currentIndex) {
                drawHealthOverlayText(
                    text = formatHealthStat(healthVal, prefs.healthMetric),
                    centerX = x + effectiveSize / 2f,
                    centerY = y + effectiveSize / 2f,
                    dotSize = effectiveSize * unitShape.textSafeBoundsScale,
                    bgArgb = bgColor.copy(alpha = 0.85f).toArgb(),
                    paint = textPaint
                )
            }
        }
    }
}

private fun resolveDotColor(
    index: Int,
    currentIndex: Int,
    pastColor: Color,
    currentColor: Color,
    pulseAlpha: Float,
    healthVal: Float?,
    healthGoal: Float
): Color = when {
    index < currentIndex -> {
        val alpha = if (healthVal != null && healthGoal > 0f) {
            val ratio = (healthVal / healthGoal).coerceIn(0f, 1f)
            HEALTH_BASE_ALPHA + HEALTH_ALPHA_SPAN * ratio
        } else {
            1f
        }
        pastColor.copy(alpha = alpha)
    }

    index == currentIndex -> currentColor.copy(alpha = pulseAlpha)
    else -> pastColor.copy(alpha = FUTURE_DOT_ALPHA)
}

private fun DrawScope.drawHealthOverlayText(
    text: String,
    centerX: Float,
    centerY: Float,
    dotSize: Float,
    bgArgb: Int,
    paint: Paint
) {
    paint.color = bgArgb
    paint.textSize = dotSize * HEALTH_TEXT_SCALE
    val baseline = centerY - (paint.descent() + paint.ascent()) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, centerX, baseline, paint)
}

private fun sampleHealthValue(metric: String, index: Int): Float = when (metric) {
    HealthConnectManager.METRIC_STEPS -> SAMPLE_STEPS[index % SAMPLE_STEPS.size]
    HealthConnectManager.METRIC_CALORIES -> SAMPLE_CALS[index % SAMPLE_CALS.size]
    HealthConnectManager.METRIC_DISTANCE -> SAMPLE_DIST[index % SAMPLE_DIST.size]
    HealthConnectManager.METRIC_SLEEP -> SAMPLE_SLEEP[index % SAMPLE_SLEEP.size]
    else -> 0f
}

private fun formatHealthStat(value: Float, metric: String): String = when (metric) {
    HealthConnectManager.METRIC_STEPS -> if (value >= THOUSAND) {
        String.format(Locale.US, "%.1fk", value / THOUSAND)
    } else {
        value.toInt().toString()
    }

    HealthConnectManager.METRIC_CALORIES -> value.toInt().toString()
    HealthConnectManager.METRIC_DISTANCE -> String.format(Locale.US, "%.1fk", value)
    HealthConnectManager.METRIC_SLEEP -> String.format(Locale.US, "%.1fh", value)
    else -> ""
}
