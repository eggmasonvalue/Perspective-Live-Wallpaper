package com.perspectivelive.wallpaper.settings.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.rendering.GridCalculator
import com.perspectivelive.wallpaper.service.HealthConnectManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val DEFAULT_PHONE_ASPECT_RATIO = 9f / 20f
private const val MIN_ASPECT_RATIO = 0.38f
private const val MAX_ASPECT_RATIO = 0.65f
private const val STAGE_HEIGHT_DP = 288
private const val NEUTRAL_MAT_DARK = 0xFF1A1A1A
private const val NEUTRAL_MAT_LIGHT = 0xFFEFEFEF
private const val MINI_PREVIEW_CORNER_DP = 22
private const val STAGE_CORNER_DP = 28
private const val PULSE_MIN_ALPHA = 0.25f
private const val PULSE_ALPHA_SPAN = 0.75f
private const val NANOS_PER_MS = 1_000_000.0
private const val FULL_CIRCLE_RADIANS = 2.0 * PI
private const val HALF_PI = PI / 2.0
private const val ROUNDED_CORNER_RATIO = 0.22f
private const val RHOMBUS_SCALE = 0.707f
private const val RHOMBUS_ROTATION_DEGREES = 45f
private const val RHOMBUS_CORNER_RATIO = 0.16f
private const val FUTURE_DOT_ALPHA = 0.28f
private const val HEALTH_BASE_ALPHA = 0.22f
private const val HEALTH_ALPHA_SPAN = 0.78f
private const val HEALTH_TEXT_SCALE = 0.28f
private const val MIN_TEXT_DOT_PX = 24f
private const val THOUSAND = 1000f
private const val LIGHT_BG_LUMINANCE_THRESHOLD = 0.5f
private const val HOME_DOCK_ICONS = 4
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
    val containerSize = LocalWindowInfo.current.containerSize
    return remember(containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val shortSide = min(containerSize.width, containerSize.height).toFloat()
            val longSide = maxOf(containerSize.width, containerSize.height).toFloat()
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
    peekMode: SurfacePeekMode,
    previewDark: Boolean,
    pulseAlpha: Float,
    onPeekModeChange: (SurfacePeekMode) -> Unit,
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
        prefs.verticalPaddingScale,
        refWidth,
        refHeight
    ) {
        GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = refWidth,
            screenHeight = refHeight,
            horizontalMarginPercent = prefs.horizontalPaddingScale,
            verticalMarginPercent = prefs.verticalPaddingScale
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
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                SurfacePeekOverlay(
                    peekMode = peekMode,
                    backgroundColor = Color(colorScheme.backgroundColor),
                    isFullscreen = false,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right: Uncluttered Stage Control Rail
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
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

                // Surface Peek Connected Toggle Group
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SURFACE PEEK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SurfacePeekSelector(
                        peekMode = peekMode,
                        onPeekModeChange = onPeekModeChange
                    )
                }

                // Paired Light / Dark Preview Toggle
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PAIRED VARIANT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PairedVariantSelector(
                        previewDark = previewDark,
                        onPreviewDarkChange = onPreviewDarkChange
                    )
                }

                // Full Screen 1:1 CTA
                FilledTonalButton(
                    onClick = onExpandFullscreen,
                    modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SurfacePeekSelector(
    peekMode: SurfacePeekMode,
    onPeekModeChange: (SurfacePeekMode) -> Unit
) {
    val entries = SurfacePeekMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        entries.forEachIndexed { index, item ->
            val label = when (item) {
                SurfacePeekMode.CLEAN -> "Clean"
                SurfacePeekMode.LOCK -> "Lock"
                SurfacePeekMode.HOME -> "Home"
            }
            ToggleButton(
                checked = peekMode == item,
                onCheckedChange = { if (it) onPeekModeChange(item) },
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
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
                .height(34.dp)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(text = "Dark", style = MaterialTheme.typography.labelSmall)
        }
        ToggleButton(
            checked = !previewDark,
            onCheckedChange = { if (it) onPreviewDarkChange(false) },
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
            Text(text = "Light", style = MaterialTheme.typography.labelSmall)
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
    peekMode: SurfacePeekMode,
    pulseAlpha: Float,
    onPeekModeChange: (SurfacePeekMode) -> Unit,
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
            SurfacePeekOverlay(
                peekMode = peekMode,
                backgroundColor = Color(colorScheme.backgroundColor),
                isFullscreen = true,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SurfacePeekMode.entries.forEach { item ->
                            val selected = item == peekMode
                            val label = when (item) {
                                SurfacePeekMode.CLEAN -> "Clean"
                                SurfacePeekMode.LOCK -> "Lock"
                                SurfacePeekMode.HOME -> "Home"
                            }
                            Surface(
                                onClick = { onPeekModeChange(item) },
                                shape = CircleShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                FilledTonalButton(onClick = onDismiss) {
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
}

@Composable
private fun SurfacePeekOverlay(
    peekMode: SurfacePeekMode,
    backgroundColor: Color,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    if (peekMode == SurfacePeekMode.CLEAN) return

    val ink = if (backgroundColor.luminance() > LIGHT_BG_LUMINANCE_THRESHOLD) {
        Color.Black.copy(alpha = 0.82f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    when (peekMode) {
        SurfacePeekMode.LOCK -> LockSurfacePeek(ink = ink, isFullscreen = isFullscreen, modifier = modifier)
        SurfacePeekMode.HOME -> HomeSurfacePeek(ink = ink, isFullscreen = isFullscreen, modifier = modifier)
        SurfacePeekMode.CLEAN -> Unit
    }
}

@Composable
private fun LockSurfacePeek(
    ink: Color,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val nowTime = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val nowDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")) }
    Column(
        modifier = modifier
            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
            .padding(top = if (isFullscreen) 48.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = nowDate,
            color = ink,
            fontSize = if (isFullscreen) 16.sp else 7.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = nowTime,
            color = ink,
            fontSize = if (isFullscreen) 84.sp else 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = if (isFullscreen) 88.sp else 30.sp
        )
    }
}

@Composable
private fun HomeSurfacePeek(
    ink: Color,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val paddingMod = if (isFullscreen) {
        Modifier.statusBarsPadding().navigationBarsPadding()
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(paddingMod)
            .padding(
                horizontal = if (isFullscreen) 24.dp else 10.dp,
                vertical = if (isFullscreen) 48.dp else 14.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 56.dp else 18.dp)
                .clip(RoundedCornerShape(if (isFullscreen) 20.dp else 6.dp))
                .background(ink.copy(alpha = 0.16f))
        )
        Column(verticalArrangement = Arrangement.spacedBy(if (isFullscreen) 18.dp else 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(HOME_DOCK_ICONS) {
                    Box(
                        modifier = Modifier
                            .size(if (isFullscreen) 52.dp else 18.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.22f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFullscreen) 44.dp else 14.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.18f))
            )
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
    val useHealth = mode == WallpaperMode.MICRO && prefs.healthMetric != HealthConnectManager.METRIC_NONE

    Canvas(modifier = modifier) {
        drawRect(color = bgColor)

        val grid = GridCalculator.calculateGridLayout(
            totalDots = gridState.totalItems,
            screenWidth = size.width.toInt(),
            screenHeight = size.height.toInt(),
            horizontalMarginPercent = prefs.horizontalPaddingScale,
            verticalMarginPercent = prefs.verticalPaddingScale
        )

        val effectiveSize = grid.dotSize * prefs.unitScale
        val innerOffset = (grid.dotSize - effectiveSize) / 2f
        val cellStep = grid.dotSize + grid.spacing
        val showOverlay = useHealth && prefs.showStatOverlay && effectiveSize >= MIN_TEXT_DOT_PX

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

            drawUnitShape(
                shapeId = prefs.unitShapeId,
                topLeft = Offset(x, y),
                dotSize = effectiveSize,
                color = dotColor
            )

            if (showOverlay && healthVal != null && index <= gridState.currentIndex) {
                drawHealthOverlayText(
                    text = formatHealthStat(healthVal, prefs.healthMetric),
                    centerX = x + effectiveSize / 2f,
                    centerY = y + effectiveSize / 2f,
                    dotSize = effectiveSize,
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

private fun DrawScope.drawUnitShape(
    shapeId: String,
    topLeft: Offset,
    dotSize: Float,
    color: Color
) {
    val center = Offset(topLeft.x + dotSize / 2f, topLeft.y + dotSize / 2f)
    when (shapeId) {
        "circle" -> drawCircle(color = color, radius = dotSize / 2f, center = center)
        "rhombus" -> {
            val side = dotSize * RHOMBUS_SCALE
            val radius = side * RHOMBUS_CORNER_RATIO
            rotate(degrees = RHOMBUS_ROTATION_DEGREES, pivot = center) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        else -> {
            val radius = dotSize * ROUNDED_CORNER_RATIO
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(dotSize, dotSize),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
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
