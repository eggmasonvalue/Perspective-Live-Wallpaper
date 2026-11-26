package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.UserPreferences
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

private data class ShapeOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val SHAPE_OPTIONS = listOf(
    ShapeOption("circle", "Circle", Icons.Rounded.Circle),
    ShapeOption("rounded_square", "Rounded", Icons.Rounded.RoundedCorner),
    ShapeOption("rhombus", "Rhombus", Icons.Rounded.ChangeHistory)
)

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Unit Shape Connected Button Group
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            SHAPE_OPTIONS.forEachIndexed { index, option ->
                val checked = prefs.unitShapeId == option.id ||
                    (option.id == "rhombus" && prefs.unitShapeId == "square")
                ToggleButton(
                    checked = checked,
                    onCheckedChange = { if (it) onShapeSelected(option.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        SHAPE_OPTIONS.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

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
