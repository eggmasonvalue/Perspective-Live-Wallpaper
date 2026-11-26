package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.UnitShape
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.rendering.UnitShapeGeometry
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_UNIT_SCALE = 0.5f
private const val MAX_UNIT_SCALE = 1.0f
private const val UNIT_SCALE_STEPS = 9
private const val MIN_PADDING = 0.0f
private const val MAX_HORIZONTAL_PADDING = 0.2f
private const val HORIZONTAL_PADDING_STEPS = 19
private const val MAX_VERTICAL_PADDING = 0.40f
private const val VERTICAL_PADDING_STEPS = 39
private const val MIN_PULSE_MS = 500f
private const val MAX_PULSE_MS = 5000f
private const val PULSE_STEP_MS = 100f
private const val PULSE_STEPS = 44
private const val PERCENT_MULTIPLIER = 100
private const val MS_PER_SECOND = 1000f
private const val SHAPE_TILE_SIZE_DP = 52
private const val SHAPE_ICON_SIZE_DP = 26
private const val SHAPE_TILE_UNSELECTED_CORNER_DP = 18
private const val SHAPE_TILE_SELECTED_CORNER_DP = 14

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CraftPanel(
    prefs: UserPreferences,
    onShapeSelected: (String) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onHorizontalPaddingChanged: (Float) -> Unit,
    onTopPaddingChanged: (Float) -> Unit,
    onBottomPaddingChanged: (Float) -> Unit,
    onPulsePeriodChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeShape = UnitShape.fromId(prefs.unitShapeId)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expressive Unit Shape Selector Strip rendering actual live silhouettes
        ShapeSelectorCard(
            activeShape = activeShape,
            onShapeSelected = { onShapeSelected(it.id) }
        )

        // Segmented Card Group: Unit Size, Horizontal Margin, Top Margin, Bottom Margin, Breathing Cycle
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SegmentedSliderCard(
                label = "Unit Size",
                valueLabel = "${(prefs.unitScale * PERCENT_MULTIPLIER).roundToInt()}%",
                value = prefs.unitScale.coerceIn(MIN_UNIT_SCALE, MAX_UNIT_SCALE),
                valueRange = MIN_UNIT_SCALE..MAX_UNIT_SCALE,
                steps = UNIT_SCALE_STEPS,
                onValueChange = onScaleChanged,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
            )

            SegmentedSliderCard(
                label = "Horizontal Margin",
                valueLabel = "${(prefs.horizontalPaddingScale * PERCENT_MULTIPLIER).roundToInt()}%",
                value = prefs.horizontalPaddingScale.coerceIn(MIN_PADDING, MAX_HORIZONTAL_PADDING),
                valueRange = MIN_PADDING..MAX_HORIZONTAL_PADDING,
                steps = HORIZONTAL_PADDING_STEPS,
                onValueChange = onHorizontalPaddingChanged,
                shape = RoundedCornerShape(6.dp)
            )

            SegmentedSliderCard(
                label = "Top Margin",
                valueLabel = "${(prefs.topPaddingScale * PERCENT_MULTIPLIER).roundToInt()}%",
                value = prefs.topPaddingScale.coerceIn(MIN_PADDING, MAX_VERTICAL_PADDING),
                valueRange = MIN_PADDING..MAX_VERTICAL_PADDING,
                steps = VERTICAL_PADDING_STEPS,
                onValueChange = onTopPaddingChanged,
                shape = RoundedCornerShape(6.dp)
            )

            SegmentedSliderCard(
                label = "Bottom Margin",
                valueLabel = "${(prefs.bottomPaddingScale * PERCENT_MULTIPLIER).roundToInt()}%",
                value = prefs.bottomPaddingScale.coerceIn(MIN_PADDING, MAX_VERTICAL_PADDING),
                valueRange = MIN_PADDING..MAX_VERTICAL_PADDING,
                steps = VERTICAL_PADDING_STEPS,
                onValueChange = onBottomPaddingChanged,
                shape = RoundedCornerShape(6.dp)
            )

            SegmentedSliderCard(
                label = "Breathing Cycle",
                valueLabel = String.format(Locale.US, "%.1fs", prefs.pulsePeriodMs / MS_PER_SECOND),
                value = prefs.pulsePeriodMs.toFloat().coerceIn(MIN_PULSE_MS, MAX_PULSE_MS),
                valueRange = MIN_PULSE_MS..MAX_PULSE_MS,
                steps = PULSE_STEPS,
                onValueChange = { raw ->
                    val snapped = ((raw / PULSE_STEP_MS).roundToInt() * PULSE_STEP_MS).toLong()
                    onPulsePeriodChanged(snapped)
                },
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeSelectorCard(
    activeShape: UnitShape,
    onShapeSelected: (UnitShape) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unit Shape",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = activeShape.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = UnitShape.entries, key = { it.id }) { shape ->
                    ShapeTile(
                        shape = shape,
                        selected = shape == activeShape,
                        onClick = { onShapeSelected(shape) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShapeTile(
    shape: UnitShape,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tileCorner by animateDpAsState(
        targetValue = if (selected) SHAPE_TILE_SELECTED_CORNER_DP.dp else SHAPE_TILE_UNSELECTED_CORNER_DP.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "shapeTileCorner"
    )
    val tileShape = RoundedCornerShape(tileCorner)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val silhouetteColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(SHAPE_TILE_SIZE_DP.dp)
                .semantics {
                    this.selected = selected
                    contentDescription = shape.label
                }
                .clip(tileShape)
                .background(containerColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = tileShape
                )
                .clickable(role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(SHAPE_ICON_SIZE_DP.dp)) {
                val glyphPath = UnitShapeGeometry.buildPath(
                    shape = shape,
                    x = 0f,
                    y = 0f,
                    size = size.minDimension
                ).asComposePath()
                drawPath(path = glyphPath, color = silhouetteColor)
            }
        }

        Text(
            text = shape.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentedSliderCard(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    shape: RoundedCornerShape
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            val sliderState = rememberSliderState(value, steps, valueRange)
            LaunchedEffect(value) {
                if (sliderState.value != value) {
                    sliderState.value = value
                }
            }
            Slider(
                state = sliderState,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
