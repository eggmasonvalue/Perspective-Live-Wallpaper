package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.CustomColorScheme
import com.perspectivelive.wallpaper.utils.OklchColor
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val DEFAULT_BG_COLOR = 0xFF161412.toInt()
private const val DEFAULT_PAST_COLOR = 0xFFA89690.toInt()
private const val DEFAULT_CURRENT_COLOR = 0xFFFF6B4A.toInt()
private const val FULL_HUE_DEGREES = 360f
private const val MAX_OKLCH_CHROMA = 0.28f
private const val CONTRAST_OFFSET = 0.05f
private const val CONTRAST_AAA = 7.0f
private const val CONTRAST_AA = 4.5f
private const val PERCENT = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAuraSheet(
    initialCustom: CustomColorScheme?,
    sampledColor: Pair<CustomColorSlot, Int>?,
    onConsumeSampledColor: () -> Unit,
    onLaunchEyeDropper: (CustomColorSlot) -> Unit,
    onApply: (CustomColorScheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        SheetValue.Hidden,
        setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val nameState = rememberTextFieldState(initialText = initialCustom?.name ?: "Custom Aura")

    var backgroundColor by remember { mutableIntStateOf(initialCustom?.backgroundColor ?: DEFAULT_BG_COLOR) }
    var pastFutureColor by remember { mutableIntStateOf(initialCustom?.pastFutureColor ?: DEFAULT_PAST_COLOR) }
    var currentColor by remember { mutableIntStateOf(initialCustom?.currentColor ?: DEFAULT_CURRENT_COLOR) }
    var activeSlot by remember { mutableStateOf(CustomColorSlot.CURRENT) }

    // Perceptual Oklch sliders for fine-tuning the currently focused color slot
    var hue by remember { mutableFloatStateOf(25f) }
    var lightness by remember { mutableFloatStateOf(0.65f) }
    var chroma by remember { mutableFloatStateOf(0.16f) }

    fun updateSlotColor(slot: CustomColorSlot, argb: Int) {
        when (slot) {
            CustomColorSlot.BACKGROUND -> backgroundColor = argb
            CustomColorSlot.PAST_FUTURE -> pastFutureColor = argb
            CustomColorSlot.CURRENT -> currentColor = argb
        }
    }

    LaunchedEffect(sampledColor) {
        if (sampledColor != null) {
            updateSlotColor(sampledColor.first, sampledColor.second)
            activeSlot = sampledColor.first
            onConsumeSampledColor()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Custom Aura",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sample any display pixel with Android 17 EyeDropper or tune in Oklch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                state = nameState,
                label = { Text("Aura Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ColorTokenSlot(
                    label = "Canvas",
                    color = Color(backgroundColor),
                    selected = activeSlot == CustomColorSlot.BACKGROUND,
                    onClick = { activeSlot = CustomColorSlot.BACKGROUND },
                    modifier = Modifier.weight(1f)
                )
                ColorTokenSlot(
                    label = "Past / Future",
                    color = Color(pastFutureColor),
                    selected = activeSlot == CustomColorSlot.PAST_FUTURE,
                    onClick = { activeSlot = CustomColorSlot.PAST_FUTURE },
                    modifier = Modifier.weight(1f)
                )
                ColorTokenSlot(
                    label = "Pulse Accent",
                    color = Color(currentColor),
                    selected = activeSlot == CustomColorSlot.CURRENT,
                    onClick = { activeSlot = CustomColorSlot.CURRENT },
                    modifier = Modifier.weight(1f)
                )
            }

            EyeDropperActionButton(
                activeSlot = activeSlot,
                onClick = { onLaunchEyeDropper(activeSlot) }
            )

            OklchTuningCard(
                hue = hue,
                lightness = lightness,
                chroma = chroma,
                onHueChange = {
                    hue = it
                    updateSlotColor(activeSlot, OklchColor.oklchToArgb(lightness, chroma, hue))
                },
                onLightnessChange = {
                    lightness = it
                    updateSlotColor(activeSlot, OklchColor.oklchToArgb(lightness, chroma, hue))
                },
                onChromaChange = {
                    chroma = it
                    updateSlotColor(activeSlot, OklchColor.oklchToArgb(lightness, chroma, hue))
                }
            )

            AuraContrastFooter(
                backgroundColor = Color(backgroundColor),
                accentColor = Color(currentColor),
                onApply = {
                    val finalName = nameState.text.toString().trim().ifEmpty { "Custom Aura" }
                    onApply(
                        CustomColorScheme(
                            name = finalName,
                            backgroundColor = backgroundColor,
                            pastFutureColor = pastFutureColor,
                            currentColor = currentColor
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun EyeDropperActionButton(
    activeSlot: CustomColorSlot,
    onClick: () -> Unit
) {
    val slotName = when (activeSlot) {
        CustomColorSlot.BACKGROUND -> "Canvas"
        CustomColorSlot.PAST_FUTURE -> "Past / Future"
        CustomColorSlot.CURRENT -> "Pulse Accent"
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.Colorize,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Sample $slotName Pixel (EyeDropper)")
    }
}

@Composable
private fun OklchTuningCard(
    hue: Float,
    lightness: Float,
    chroma: Float,
    onHueChange: (Float) -> Unit,
    onLightnessChange: (Float) -> Unit,
    onChromaChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OklchSliderRow(
                label = "Hue",
                readout = "${hue.roundToInt()}°",
                value = hue,
                range = 0f..FULL_HUE_DEGREES,
                onValueChange = onHueChange
            )
            OklchSliderRow(
                label = "Lightness",
                readout = "${(lightness * PERCENT).roundToInt()}%",
                value = lightness,
                range = 0.05f..0.98f,
                onValueChange = onLightnessChange
            )
            OklchSliderRow(
                label = "Chroma",
                readout = String.format(Locale.US, "%.2f", chroma),
                value = chroma,
                range = 0f..MAX_OKLCH_CHROMA,
                onValueChange = onChromaChange
            )
        }
    }
}

@Composable
private fun AuraContrastFooter(
    backgroundColor: Color,
    accentColor: Color,
    onApply: () -> Unit
) {
    val contrast = calculateContrastRatio(backgroundColor, accentColor)
    val contrastGrade = when {
        contrast >= CONTRAST_AAA -> "AAA"
        contrast >= CONTRAST_AA -> "AA"
        else -> "Low"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = String.format(Locale.US, "Contrast %.1f:1 · %s", contrast, contrastGrade),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        Button(onClick = onApply) {
            Text("Apply Aura")
        }
    }
}

@Composable
private fun ColorTokenSlot(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape
            )
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OklchSliderRow(
    label: String,
    readout: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = readout,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        val sliderState = rememberSliderState(value, 0, range)
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

private fun calculateContrastRatio(first: Color, second: Color): Float {
    val l1 = first.luminance()
    val l2 = second.luminance()
    return (max(l1, l2) + CONTRAST_OFFSET) / (min(l1, l2) + CONTRAST_OFFSET)
}
