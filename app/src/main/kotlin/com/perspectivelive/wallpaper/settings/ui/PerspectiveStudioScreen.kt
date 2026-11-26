package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.ColorScheme
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.CustomColorScheme
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.ProceduralThemeGenerator
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.modules.DayCounterModule
import com.perspectivelive.wallpaper.modules.LifeCalendarModule
import java.time.LocalDate

private const val DOCK_BOTTOM_CLEARANCE_DP = 104

data class StudioActions(
    val onBirthDateSelected: (LocalDate) -> Unit,
    val onLifespanChanged: (Int) -> Unit,
    val onSelectNoTomorrow: () -> Unit,
    val onSelectVsYesterday: () -> Unit,
    val onSelectThisYear: (Boolean) -> Unit,
    val onSelectCustomObjective: () -> Unit,
    val onEventNameChanged: (String) -> Unit,
    val onStartDateSelected: (LocalDate) -> Unit,
    val onTargetDateSelected: (LocalDate) -> Unit,
    val onHealthMetricSelected: (HealthMetricSpec) -> Unit,
    val onHealthGoalChanged: (Float) -> Unit,
    val onShowStatOverlayChanged: (Boolean) -> Unit,
    val onSchemeSelected: (String) -> Unit,
    val onDailyRotationChanged: (Boolean) -> Unit,
    val onSaveCustomAura: (CustomColorScheme) -> Unit,
    val onShapeSelected: (String) -> Unit,
    val onScaleChanged: (Float) -> Unit,
    val onHorizontalPaddingChanged: (Float) -> Unit,
    val onTopPaddingChanged: (Float) -> Unit,
    val onBottomPaddingChanged: (Float) -> Unit,
    val onPulsePeriodChanged: (Long) -> Unit,
    val onLaunchEyeDropper: (CustomColorSlot) -> Unit,
    val onApplyWallpaper: (WallpaperMode) -> Unit,
    val onOpenWallpaperPicker: () -> Unit
)

@Composable
fun PerspectiveStudioScreen(
    prefs: UserPreferences,
    customColors: CustomColorScheme?,
    activeWallpaperModes: Set<WallpaperMode>,
    sampledColor: Pair<CustomColorSlot, Int>?,
    onConsumeSampledColor: () -> Unit,
    snackbarHostState: SnackbarHostState,
    actions: StudioActions
) {
    val systemDark = isSystemInDarkTheme()
    var activeMode by rememberSaveable { mutableStateOf(WallpaperMode.MACRO) }
    var activeTab by rememberSaveable { mutableStateOf(StudioTab.HORIZON) }
    var previewDark by rememberSaveable { mutableStateOf(systemDark) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showCustomAuraSheet by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val gridState = remember(prefs, activeMode, today) {
        computePreviewGridState(prefs = prefs, mode = activeMode, today = today)
    }
    val resolvedScheme = remember(prefs.colorSchemeId, prefs.isDailyRotationEnabled, previewDark, customColors, today) {
        resolvePreviewColorScheme(
            prefs = prefs,
            customColors = customColors,
            isDark = previewDark,
            today = today
        )
    }
    val (themeTitle, themeSubtitle) = remember(
        prefs.colorSchemeId,
        prefs.isDailyRotationEnabled,
        resolvedScheme,
        previewDark,
        today
    ) {
        resolveThemeLabels(
            prefs = prefs,
            resolvedScheme = resolvedScheme,
            isDark = previewDark,
            today = today
        )
    }
    val pulseAlpha = rememberBreathingAlpha(prefs.pulsePeriodMs)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 84.dp)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    StudioTopHeader(
                        activeMode = activeMode,
                        onModeChange = { activeMode = it }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Pinned true-aspect live preview stage (never scrolls out of view)
                    WallpaperPreviewStage(
                        prefs = prefs,
                        mode = activeMode,
                        gridState = gridState,
                        colorScheme = resolvedScheme,
                        themeDisplayName = themeTitle,
                        previewDark = previewDark,
                        pulseAlpha = pulseAlpha,
                        onPreviewDarkChange = { previewDark = it },
                        onExpandFullscreen = { isFullscreen = true }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Scrollable studio deck below the pinned preview
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = DOCK_BOTTOM_CLEARANCE_DP.dp)
                    ) {
                        Crossfade(targetState = activeTab, label = "studioDeckTab") { tab ->
                            when (tab) {
                                StudioTab.HORIZON -> HorizonPanel(
                                    prefs = prefs,
                                    mode = activeMode,
                                    onBirthDateSelected = actions.onBirthDateSelected,
                                    onLifespanChanged = actions.onLifespanChanged,
                                    onSelectNoTomorrow = actions.onSelectNoTomorrow,
                                    onSelectVsYesterday = actions.onSelectVsYesterday,
                                    onSelectThisYear = actions.onSelectThisYear,
                                    onSelectCustomObjective = actions.onSelectCustomObjective,
                                    onEventNameChanged = actions.onEventNameChanged,
                                    onStartDateSelected = actions.onStartDateSelected,
                                    onTargetDateSelected = actions.onTargetDateSelected,
                                    onHealthMetricSelected = actions.onHealthMetricSelected,
                                    onHealthGoalChanged = actions.onHealthGoalChanged,
                                    onShowStatOverlayChanged = actions.onShowStatOverlayChanged
                                )

                                StudioTab.PALETTE -> PalettePanel(
                                    prefs = prefs,
                                    customColors = customColors,
                                    previewDark = previewDark,
                                    activeThemeTitle = themeTitle,
                                    activeThemeSubtitle = themeSubtitle,
                                    onSchemeSelected = actions.onSchemeSelected,
                                    onDailyRotationChanged = actions.onDailyRotationChanged,
                                    onOpenCustomAura = { showCustomAuraSheet = true }
                                )

                                StudioTab.CRAFT -> CraftPanel(
                                    prefs = prefs,
                                    onShapeSelected = actions.onShapeSelected,
                                    onScaleChanged = actions.onScaleChanged,
                                    onHorizontalPaddingChanged = actions.onHorizontalPaddingChanged,
                                    onTopPaddingChanged = actions.onTopPaddingChanged,
                                    onBottomPaddingChanged = actions.onBottomPaddingChanged,
                                    onPulsePeriodChanged = actions.onPulsePeriodChanged
                                )
                            }
                        }
                    }
                }

                // Floating M3 Expressive bottom dock
                StudioFloatingDock(
                    activeTab = activeTab,
                    activeMode = activeMode,
                    isActiveModeApplied = activeMode in activeWallpaperModes,
                    onTabSelected = { activeTab = it },
                    onApplyWallpaper = actions.onApplyWallpaper,
                    onOpenWallpaperPicker = actions.onOpenWallpaperPicker,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Tap-to-expand 1:1 fullscreen live preview
        FullscreenWallpaperPreview(
            visible = isFullscreen,
            prefs = prefs,
            mode = activeMode,
            gridState = gridState,
            colorScheme = resolvedScheme,
            pulseAlpha = pulseAlpha,
            onDismiss = { isFullscreen = false }
        )

        if (showCustomAuraSheet) {
            CustomAuraSheet(
                initialCustom = customColors,
                sampledColor = sampledColor,
                onConsumeSampledColor = onConsumeSampledColor,
                onLaunchEyeDropper = actions.onLaunchEyeDropper,
                onApply = { custom ->
                    actions.onSaveCustomAura(custom)
                    showCustomAuraSheet = false
                },
                onDismiss = { showCustomAuraSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudioTopHeader(
    activeMode: WallpaperMode,
    onModeChange: (WallpaperMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Perspective",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            ToggleButton(
                checked = activeMode == WallpaperMode.MACRO,
                onCheckedChange = { if (it) onModeChange(WallpaperMode.MACRO) },
                modifier = Modifier
                    .height(36.dp)
                    .semantics { role = Role.RadioButton },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(text = "Macro", style = MaterialTheme.typography.labelLarge)
            }

            ToggleButton(
                checked = activeMode == WallpaperMode.MICRO,
                onCheckedChange = { if (it) onModeChange(WallpaperMode.MICRO) },
                modifier = Modifier
                    .height(36.dp)
                    .semantics { role = Role.RadioButton },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                Text(text = "Micro", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun computePreviewGridState(
    prefs: UserPreferences,
    mode: WallpaperMode,
    today: LocalDate
): GridState = when (mode) {
    WallpaperMode.MACRO -> {
        val module = LifeCalendarModule()
        val total = module.calculateTotalItems(prefs).coerceAtLeast(1)
        val past = module.calculatePastItems(prefs, today).coerceIn(0, total)
        val current = module.calculateCurrentItemIndex(prefs, today).coerceIn(0, total - 1)
        GridState(
            totalItems = total,
            pastItems = past,
            currentIndex = current,
            futureItems = (total - past - 1).coerceAtLeast(0),
            startDate = prefs.birthDate,
            expectedTotal = total
        )
    }

    WallpaperMode.MICRO -> {
        val module = DayCounterModule()
        val total = module.calculateTotalItems(prefs, today).coerceAtLeast(1)
        val past = module.calculatePastItems(prefs, today).coerceIn(0, total)
        val current = module.calculateCurrentItemIndex(prefs, today).coerceIn(0, total - 1)
        GridState(
            totalItems = total,
            pastItems = past,
            currentIndex = current,
            futureItems = (total - past - 1).coerceAtLeast(0),
            startDate = module.getEffectiveStartDate(prefs, today),
            expectedTotal = total
        )
    }
}

private fun resolvePreviewColorScheme(
    prefs: UserPreferences,
    customColors: CustomColorScheme?,
    isDark: Boolean,
    today: LocalDate
): ColorScheme {
    val activeId = if (prefs.isDailyRotationEnabled) {
        ColorSchemeProvider.getRotatedSchemeId(today)
    } else {
        prefs.colorSchemeId
    }
    if (activeId == "custom" && customColors != null) {
        return ColorSchemeProvider.createCustomColorScheme(customColors, isDark)
    }
    return ColorSchemeProvider.getScheme(id = activeId, isDarkMode = isDark, date = today)
}

private fun resolveThemeLabels(
    prefs: UserPreferences,
    resolvedScheme: ColorScheme,
    isDark: Boolean,
    today: LocalDate
): Pair<String, String> {
    val variantLabel = if (isDark) "Dark variant" else "Light variant"
    return when {
        prefs.isDailyRotationEnabled -> {
            "Daily Rotation · ${resolvedScheme.name}" to "Auto-cycles at midnight · $variantLabel"
        }

        prefs.colorSchemeId == ColorSchemeProvider.ATMOSPHERE_SCHEME_ID -> {
            val mood = ProceduralThemeGenerator.getMoodName(today)
            "Atmosphere" to "Today: $mood · Oklch procedural"
        }

        resolvedScheme.isCustom -> {
            resolvedScheme.name to "Custom Aura palette"
        }

        else -> {
            resolvedScheme.name to "Paired Light & Dark · $variantLabel"
        }
    }
}
