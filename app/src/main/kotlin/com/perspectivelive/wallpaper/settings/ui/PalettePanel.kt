package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerRounding
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.CustomColorScheme
import com.perspectivelive.wallpaper.data.ProceduralThemeGenerator
import com.perspectivelive.wallpaper.data.UserPreferences
import java.time.LocalDate

private const val ORB_SIZE_DP = 52
private const val ORB_CIRCLE_CORNER_DP = 26
private const val ORB_SELECTED_CORNER_DP = 15
private const val ATMOSPHERE_STAR_POINTS = 10
private const val ATMOSPHERE_INNER_RATIO = 0.84f
private const val ATMOSPHERE_ROUNDING = 0.45f
private const val ATMOSPHERE_ACCENT_RADIUS_RATIO = 0.24f
private const val LIGHT_LUMINANCE_THRESHOLD = 0.5f

/**
 * Option 1 Palette Studio: Pure Pixel-style 3-tone Color Orbs with zero redundant cards or mini-grids.
 * Active theme name is displayed once cleanly above the orb strip; active orb morphs circle -> squircle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PalettePanel(
    prefs: UserPreferences,
    customColors: CustomColorScheme?,
    previewDark: Boolean,
    activeThemeTitle: String,
    activeThemeSubtitle: String,
    onSchemeSelected: (String) -> Unit,
    onDailyRotationChanged: (Boolean) -> Unit,
    onOpenCustomAura: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val schemes = remember(previewDark, customColors, today) {
        val all = ColorSchemeProvider.getAllSchemes(isDarkMode = previewDark, date = today)
        val atmosphere = all.firstOrNull { it.id == ColorSchemeProvider.ATMOSPHERE_SCHEME_ID }
        val curated = all.filter { it.id != ColorSchemeProvider.ATMOSPHERE_SCHEME_ID }
        val customScheme = customColors?.let { ColorSchemeProvider.createCustomColorScheme(it, previewDark) }
        buildList {
            if (atmosphere != null) add(atmosphere)
            addAll(curated)
            if (customScheme != null) add(customScheme)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Theme Title (shown ONCE cleanly) + Custom Aura EyeDropper CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeThemeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeThemeSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            FilledTonalButton(
                onClick = onOpenCustomAura,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Colorize,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(text = "Custom", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Pure Horizontal Strip of 3-Tone Pixel Color Orbs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items = schemes, key = { it.id }) { scheme ->
                    val isSelected = !prefs.isDailyRotationEnabled && prefs.colorSchemeId == scheme.id
                    if (scheme.id == ColorSchemeProvider.ATMOSPHERE_SCHEME_ID) {
                        AtmosphereScallopOrb(
                            scheme = scheme,
                            selected = isSelected,
                            onClick = { onSchemeSelected(scheme.id) }
                        )
                    } else {
                        TriToneColorOrb(
                            scheme = scheme,
                            selected = isSelected,
                            onClick = { onSchemeSelected(scheme.id) }
                        )
                    }
                }
            }
        }

        // Daily Rotation Compact Segmented Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Rotation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val subtitle = if (prefs.isDailyRotationEnabled) {
                        val todayName = ColorSchemeProvider.getScheme(
                            id = ColorSchemeProvider.getRotatedSchemeId(today),
                            isDarkMode = previewDark
                        ).name
                        "Active · Today: $todayName"
                    } else {
                        "Cycle 14 curated palettes at midnight"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                ExpressiveCheckSwitch(
                    checked = prefs.isDailyRotationEnabled,
                    onCheckedChange = onDailyRotationChanged
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TriToneColorOrb(
    scheme: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) ORB_SELECTED_CORNER_DP.dp else ORB_CIRCLE_CORNER_DP.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "orbCornerMorph"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val bgColor = Color(scheme.backgroundColor)
    val pastColor = Color(scheme.pastYearsColor)
    val accentColor = Color(scheme.currentYearColor)

    Box(
        modifier = Modifier
            .size(ORB_SIZE_DP.dp)
            .semantics {
                this.selected = selected
                contentDescription = scheme.name
            }
            .clip(shape)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape
            )
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfH = size.height / 2f
            val halfW = size.width / 2f
            // Top semicircle: Background canvas
            drawRect(color = bgColor, topLeft = Offset.Zero, size = Size(size.width, halfH))
            // Bottom-left quarter: Past/Future dots
            drawRect(color = pastColor, topLeft = Offset(0f, halfH), size = Size(halfW, halfH))
            // Bottom-right quarter: Present pulse accent
            drawRect(color = accentColor, topLeft = Offset(halfW, halfH), size = Size(halfW, halfH))
        }

        if (selected) {
            OrbSelectionCheck(bgColor = bgColor)
        }
    }
}

@Composable
private fun AtmosphereScallopOrb(
    scheme: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scallopShape = remember {
        PolygonShape.star(
            numPoints = ATMOSPHERE_STAR_POINTS,
            innerRadiusRatio = ATMOSPHERE_INNER_RATIO,
            outerRounding = CornerRounding.fraction(ATMOSPHERE_ROUNDING)
        )
    }
    val pastColor = Color(scheme.pastYearsColor)
    val accentColor = Color(scheme.currentYearColor)
    val mood = remember { ProceduralThemeGenerator.getMoodName() }

    Box(
        modifier = Modifier
            .size(ORB_SIZE_DP.dp)
            .semantics {
                this.selected = selected
                contentDescription = "${scheme.name}: $mood"
            }
            .clip(scallopShape)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = scallopShape
            )
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = pastColor)
            drawCircle(
                color = accentColor,
                radius = size.minDimension * ATMOSPHERE_ACCENT_RADIUS_RATIO,
                center = center
            )
        }

        if (selected) {
            OrbSelectionCheck(bgColor = accentColor)
        }
    }
}

@Composable
private fun OrbSelectionCheck(bgColor: Color) {
    val badgeBg = if (bgColor.luminance() > LIGHT_LUMINANCE_THRESHOLD) {
        Color.Black.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }
    val iconTint = if (bgColor.luminance() > LIGHT_LUMINANCE_THRESHOLD) Color.White else Color.Black

    Surface(
        shape = CircleShape,
        color = badgeBg,
        modifier = Modifier.size(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
