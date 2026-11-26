package com.perspectivelive.wallpaper.settings

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.PreferencesManager
import com.perspectivelive.wallpaper.service.DayCounterService
import com.perspectivelive.wallpaper.service.HealthConnectManager
import com.perspectivelive.wallpaper.service.LifeCalendarService
import com.perspectivelive.wallpaper.settings.theme.PerspectiveTheme
import com.perspectivelive.wallpaper.settings.ui.CustomColorSlot
import com.perspectivelive.wallpaper.settings.ui.HealthMetricSpec
import com.perspectivelive.wallpaper.settings.ui.PerspectiveStudioScreen
import com.perspectivelive.wallpaper.settings.ui.StudioActions
import com.perspectivelive.wallpaper.settings.ui.WallpaperMode
import kotlinx.coroutines.launch

/**
 * Companion studio Activity for Perspective - Live built with Jetpack Compose
 * and Material 3 Expressive 1.5.0 targeting Android 17 (API 37).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(PreferencesManager(this))
    }

    private var pendingHealthSpec: HealthMetricSpec? = null
    private var pendingEyeDropperSlot: CustomColorSlot? = null
    private var sampledColorState by mutableStateOf<Pair<CustomColorSlot, Int>?>(null)

    private val snackbarHostState = SnackbarHostState()

    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val spec = pendingHealthSpec ?: return@registerForActivityResult
        pendingHealthSpec = null
        val required = HealthConnectManager.getRequiredPermission(spec.id)
        if (required == null || grantedPermissions.contains(required)) {
            viewModel.updateHealthMetric(spec.id, spec.defaultGoal)
        } else {
            showSnackbar("Health Connect permission is required for ${spec.label}")
        }
    }

    private val eyeDropperLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val slot = pendingEyeDropperSlot ?: return@registerForActivityResult
        pendingEyeDropperSlot = null
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedColor = result.data?.getIntExtra(Intent.EXTRA_COLOR, Color.BLACK)
                ?: return@registerForActivityResult
            sampledColorState = slot to pickedColor
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PerspectiveTheme {
                val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
                val customColors by viewModel.customColors.collectAsStateWithLifecycle()

                val actions = remember(viewModel) {
                    StudioActions(
                        onBirthDateSelected = { date ->
                            if (!viewModel.updateBirthDate(date)) {
                                showSnackbar(getString(R.string.birth_date_validation_error))
                            }
                        },
                        onLifespanChanged = viewModel::updateExpectedLifespan,
                        onSelectNoTomorrow = viewModel::setNoTomorrowMode,
                        onSelectVsYesterday = viewModel::setVsYesterdayMode,
                        onSelectThisYear = viewModel::setThisYearMode,
                        onSelectCustomObjective = viewModel::setCustomObjectiveMode,
                        onEventNameChanged = viewModel::updateEventName,
                        onStartDateSelected = { date ->
                            if (!viewModel.updateStartDate(date)) {
                                showSnackbar("Start date cannot be after target date")
                            }
                        },
                        onTargetDateSelected = { date ->
                            if (!viewModel.updateEventDate(date)) {
                                showSnackbar(getString(R.string.event_date_validation_error))
                            }
                        },
                        onHealthMetricSelected = ::handleHealthMetricSelected,
                        onHealthGoalChanged = viewModel::updateHealthGoal,
                        onShowStatOverlayChanged = viewModel::updateShowStatOverlay,
                        onSchemeSelected = viewModel::selectColorScheme,
                        onDailyRotationChanged = viewModel::setDailyRotationEnabled,
                        onSaveCustomAura = viewModel::saveCustomColors,
                        onShapeSelected = viewModel::updateUnitShape,
                        onScaleChanged = viewModel::updateUnitScale,
                        onHorizontalPaddingChanged = viewModel::updateHorizontalPaddingScale,
                        onVerticalPaddingChanged = viewModel::updateVerticalPaddingScale,
                        onPulsePeriodChanged = viewModel::updatePulsePeriod,
                        onLaunchEyeDropper = ::launchEyeDropper,
                        onApplyWallpaper = ::applyWallpaper
                    )
                }

                PerspectiveStudioScreen(
                    prefs = prefs,
                    customColors = customColors,
                    sampledColor = sampledColorState,
                    onConsumeSampledColor = { sampledColorState = null },
                    snackbarHostState = snackbarHostState,
                    actions = actions
                )
            }
        }
    }

    private fun handleHealthMetricSelected(spec: HealthMetricSpec) {
        if (spec.id == HealthConnectManager.METRIC_NONE) {
            viewModel.updateHealthMetric(HealthConnectManager.METRIC_NONE)
            return
        }
        lifecycleScope.launch {
            val manager = HealthConnectManager(this@MainActivity)
            if (manager.hasPermissions(spec.id)) {
                viewModel.updateHealthMetric(spec.id, spec.defaultGoal)
            } else {
                val perm = HealthConnectManager.getRequiredPermission(spec.id)
                if (perm != null) {
                    pendingHealthSpec = spec
                    healthPermissionLauncher.launch(setOf(perm))
                } else {
                    viewModel.updateHealthMetric(spec.id, spec.defaultGoal)
                }
            }
        }
    }

    private fun launchEyeDropper(slot: CustomColorSlot) {
        pendingEyeDropperSlot = slot
        val intent = Intent(Intent.ACTION_OPEN_EYE_DROPPER)
        runCatching { eyeDropperLauncher.launch(intent) }
            .onFailure {
                pendingEyeDropperSlot = null
                showSnackbar("System EyeDropper is unavailable on this display")
            }
    }

    private fun applyWallpaper(mode: WallpaperMode) {
        viewModel.savePreferences()
        val serviceClass = when (mode) {
            WallpaperMode.MACRO -> LifeCalendarService::class.java
            WallpaperMode.MICRO -> DayCounterService::class.java
        }
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, serviceClass)
            )
        }
        startActivity(intent)
    }

    private fun showSnackbar(message: String) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}
