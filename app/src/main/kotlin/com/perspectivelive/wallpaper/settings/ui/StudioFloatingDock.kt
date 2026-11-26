package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val CHEVRON_OPEN_DEGREES = 180f

private data class DockDestination(
    val tab: StudioTab,
    val label: String,
    val icon: ImageVector
)

private val DOCK_DESTINATIONS = listOf(
    DockDestination(StudioTab.HORIZON, "Horizon", Icons.Rounded.Schedule),
    DockDestination(StudioTab.PALETTE, "Palette", Icons.Rounded.Palette),
    DockDestination(StudioTab.CRAFT, "Craft", Icons.Rounded.Tune)
)

/**
 * Floating M3 Expressive 1.5.0 bottom dock pairing a vibrant HorizontalFloatingToolbar
 * with a detached SplitButtonLayout that opens Wallpaper & style when the selected mode
 * is already active, or directly applies the live wallpaper otherwise.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudioFloatingDock(
    activeTab: StudioTab,
    activeMode: WallpaperMode,
    isActiveModeApplied: Boolean,
    onTabSelected: (StudioTab) -> Unit,
    onApplyWallpaper: (WallpaperMode) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            content = {
                DOCK_DESTINATIONS.forEach { dest ->
                    val selected = activeTab == dest.tab
                    ToggleButton(
                        checked = selected,
                        onCheckedChange = { if (it) onTabSelected(dest.tab) },
                        modifier = Modifier.semantics { role = Role.Tab },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            modifier = Modifier.size(18.dp)
                        )
                        if (selected) {
                            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        )

        Box {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = {
                            if (isActiveModeApplied) {
                                onOpenWallpaperPicker()
                            } else {
                                onApplyWallpaper(activeMode)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isActiveModeApplied) Icons.Rounded.Wallpaper else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = if (isActiveModeApplied) "Preview" else "Set",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                trailingButton = {
                    val rotation by animateFloatAsState(
                        targetValue = if (menuExpanded) CHEVRON_OPEN_DEGREES else 0f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "splitChevronRotation"
                    )
                    SplitButtonDefaults.TrailingButton(
                        checked = menuExpanded,
                        onCheckedChange = { menuExpanded = it }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Wallpaper actions",
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Open Wallpaper & style") },
                    onClick = {
                        menuExpanded = false
                        onOpenWallpaperPicker()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set Macro (Life Horizon)") },
                    onClick = {
                        menuExpanded = false
                        onApplyWallpaper(WallpaperMode.MACRO)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Set Micro (Momentum)") },
                    onClick = {
                        menuExpanded = false
                        onApplyWallpaper(WallpaperMode.MICRO)
                    }
                )
            }
        }
    }
}
