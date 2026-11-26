package com.perspectivelive.wallpaper.service

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.perspectivelive.wallpaper.R
import com.perspectivelive.wallpaper.data.ColorSchemeProvider
import com.perspectivelive.wallpaper.data.GridState
import com.perspectivelive.wallpaper.data.PreferencesManager
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.rendering.CanvasRenderer
import com.perspectivelive.wallpaper.rendering.PulseAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Base WallpaperService that encapsulates lifecycle, hardware-accelerated rendering,
 * frame-rate pacing, power-save awareness, and midnight rollover logic.
 */
abstract class BaseWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "BaseWallpaperService"
        private const val MAX_CONSECUTIVE_ERRORS = 5
        private const val PLACEHOLDER_COLOR = 0xFF0A0A0A.toInt()
        private const val DEFAULT_PULSE_PERIOD = 2000L
    }

    private val activeEngines = mutableSetOf<BaseEngine>()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activeEngines.forEach { it.onSystemConfigurationChanged(newConfig) }
    }

    override fun onCreateEngine(): Engine {
        return createBaseEngine()
    }

    abstract fun createBaseEngine(): BaseEngine

    @Suppress("TooManyFunctions")
    abstract inner class BaseEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        protected lateinit var preferencesManager: PreferencesManager
        protected var renderer: CanvasRenderer? = null
        protected lateinit var animator: PulseAnimator
        protected lateinit var scheduler: MidnightScheduler
        protected val handler = Handler(Looper.getMainLooper())

        protected var isRenderingVisible: Boolean = false
        protected var surfaceWidth: Int = 0
        protected var surfaceHeight: Int = 0
        protected var isSafeMode: Boolean = false
        protected var isPowerSaveMode: Boolean = false
        protected var lastRenderedDate: LocalDate? = null

        private var consecutiveErrors = 0
        private var cachedTypeface: Typeface? = null

        private val frameRunnable = Runnable {
            if (!isRenderingVisible || isSafeMode || isPowerSaveMode) return@Runnable
            drawFrame()
            scheduleNextFrame()
        }

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MidnightReceiver.ACTION_UPDATE_WALLPAPER && hasPreferences()) {
                    lastRenderedDate = LocalDate.now()
                    performMidnightUpdate(preferencesManager.getPreferences())
                    scheduler.scheduleMidnightCheck()
                }
            }
        }

        private val powerSaveReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    syncPowerSaveMode()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            activeEngines.add(this)

            // Disable unnecessary IPC notifications from launcher/window manager
            setOffsetNotificationsEnabled(false)
            setTouchEventsEnabled(false)

            preferencesManager = PreferencesManager(this@BaseWallpaperService)
            val pulsePeriod = runCatching { preferencesManager.getPreferences().pulsePeriodMs }
                .getOrDefault(DEFAULT_PULSE_PERIOD)
            animator = PulseAnimator(pulsePeriod)
            scheduler = MidnightScheduler(this@BaseWallpaperService)

            val prefs = getSharedPreferences("life_calendar_prefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)

            registerEngineReceivers()
            syncPowerSaveMode(forceStaticDrawIfActive = false)
            scheduler.scheduleMidnightCheck()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height

            // Hint display subsystem and LTPO panels to run at 30Hz target frame rate
            runCatching {
                holder.surface.setFrameRate(
                    PulseAnimator.TARGET_FPS.toFloat(),
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                )
            }.onFailure { Log.w(TAG, "Could not set surface frame rate", it) }

            initializeRendererAsync()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isRenderingVisible = false
            stopFrameSchedule()
            renderer = null
        }

        open fun onSystemConfigurationChanged(newConfig: Configuration) {
            initializeRendererAsync()
        }

        override fun onComputeColors(): WallpaperColors? {
            val scheme = renderer?.colorScheme ?: return null
            return WallpaperColors(
                Color.valueOf(scheme.pastYearsColor),
                Color.valueOf(scheme.currentYearColor),
                Color.valueOf(scheme.backgroundColor)
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isRenderingVisible = visible

            if (visible) {
                syncPowerSaveMode(forceStaticDrawIfActive = false)
                animator.reset()

                val today = LocalDate.now()
                if (lastRenderedDate != null && today != lastRenderedDate) {
                    lastRenderedDate = today
                    performMidnightUpdate(preferencesManager.getPreferences())
                } else if (renderer == null && !isSafeMode) {
                    initializeRendererAsync()
                }

                if (isPowerSaveMode) {
                    drawFrame(forcedOpacity = 1.0f)
                } else {
                    scheduleNextFrame()
                }
            } else {
                stopFrameSchedule()
            }
        }

        override fun onDestroy() {
            activeEngines.remove(this)
            stopFrameSchedule()
            scheduler.cancel()

            val prefs = getSharedPreferences("life_calendar_prefs", Context.MODE_PRIVATE)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            unregisterEngineReceivers()

            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            handler.post {
                stopFrameSchedule()
                initializeRendererAsync()
            }
        }

        protected open fun initializeRendererAsync() {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    initializeRenderer()
                }
            }
        }

        protected open fun initializeRenderer(healthCache: Map<LocalDate, Float>? = null) {
            if (!hasPreferences()) {
                drawPlaceholder()
                return
            }

            try {
                val preferences = preferencesManager.getPreferences()
                val gridState = getGridState(preferences) ?: return
                setupRendererInstance(preferences, gridState, healthCache)

                consecutiveErrors = 0
                isSafeMode = false
                lastRenderedDate = LocalDate.now()

                notifyColorsChanged()

                if (isPowerSaveMode) {
                    drawFrame(forcedOpacity = 1.0f)
                } else {
                    drawFrame()
                    if (isRenderingVisible) {
                        scheduleNextFrame()
                    }
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "State invalid during renderer initialization", e)
                drawPlaceholder()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid argument initializing renderer", e)
                handleError()
            }
        }

        private fun setupRendererInstance(
            preferences: UserPreferences,
            gridState: GridState,
            healthCache: Map<LocalDate, Float>?
        ) {
            val activeSchemeId = if (preferences.isDailyRotationEnabled) {
                ColorSchemeProvider.getRotatedSchemeId(LocalDate.now())
            } else {
                preferences.colorSchemeId
            }

            val isDarkMode = ColorSchemeProvider.isSystemDarkMode(this@BaseWallpaperService)
            val colorScheme = ColorSchemeProvider.getScheme(
                id = activeSchemeId,
                isDarkMode = isDarkMode,
                prefsManager = preferencesManager
            )

            val typeface = cachedTypeface ?: runCatching {
                ResourcesCompat.getFont(this@BaseWallpaperService, R.font.geist_bold)
                    ?: ResourcesCompat.getFont(this@BaseWallpaperService, R.font.geist)
            }.getOrNull().also { cachedTypeface = it }

            animator.cycleDurationMs = preferences.pulsePeriodMs
            renderer = CanvasRenderer(
                gridState = gridState,
                colorScheme = colorScheme,
                screenWidth = surfaceWidth,
                screenHeight = surfaceHeight,
                customTypeface = typeface
            ).apply {
                updateStyle(
                    preferences.unitShapeId,
                    preferences.unitScale,
                    preferences.horizontalPaddingScale,
                    preferences.topPaddingScale,
                    preferences.bottomPaddingScale
                )
                if (preferences.healthMetric != "NONE") {
                    updateHealthData(
                        preferences.healthMetric,
                        preferences.healthMetricGoal,
                        preferences.showStatOverlay,
                        healthCache ?: emptyMap()
                    )
                }
            }
        }

        protected abstract fun hasPreferences(): Boolean
        protected abstract fun getGridState(preferences: UserPreferences): GridState?
        protected abstract fun performMidnightUpdate(preferences: UserPreferences)

        private fun scheduleNextFrame() {
            if (!isRenderingVisible || isSafeMode || isPowerSaveMode) return
            handler.removeCallbacks(frameRunnable)
            handler.postDelayed(frameRunnable, PulseAnimator.FRAME_DURATION_MS)
        }

        private fun stopFrameSchedule() {
            handler.removeCallbacks(frameRunnable)
        }

        private fun drawFrame(forcedOpacity: Float? = null) {
            if (renderer == null && !isSafeMode) return

            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            var canvas: Canvas? = null
            try {
                canvas = runCatching { holder.lockHardwareCanvas() }
                    .getOrElse { holder.lockCanvas() }

                if (canvas != null) {
                    if (isSafeMode) {
                        canvas.drawColor(Color.BLACK)
                    } else {
                        val pulseOpacity = forcedOpacity ?: animator.getCurrentOpacity()
                        renderer?.render(canvas, pulseOpacity)
                    }
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Surface state invalid in drawFrame", e)
                handleError()
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Drawing argument error in drawFrame", e)
                handleError()
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                        .onFailure { Log.w(TAG, "Error unlocking canvas", it) }
                }
            }
        }

        protected fun drawPlaceholder() {
            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            var canvas: Canvas? = null
            try {
                canvas = runCatching { holder.lockHardwareCanvas() }
                    .getOrElse { holder.lockCanvas() }
                canvas?.drawColor(PLACEHOLDER_COLOR)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Error drawing placeholder", e)
            } finally {
                if (canvas != null) {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                        .onFailure { Log.w(TAG, "Error unlocking placeholder canvas", it) }
                }
            }
        }

        private fun syncPowerSaveMode(forceStaticDrawIfActive: Boolean = true) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            val newMode = powerManager?.isPowerSaveMode == true
            if (newMode != isPowerSaveMode) {
                isPowerSaveMode = newMode
                if (isPowerSaveMode) {
                    stopFrameSchedule()
                    if (forceStaticDrawIfActive && isRenderingVisible) {
                        drawFrame(forcedOpacity = 1.0f)
                    }
                } else if (isRenderingVisible && !isSafeMode) {
                    scheduleNextFrame()
                }
            }
        }

        private fun registerEngineReceivers() {
            val midnightFilter = IntentFilter(MidnightReceiver.ACTION_UPDATE_WALLPAPER)
            registerReceiver(updateReceiver, midnightFilter, Context.RECEIVER_NOT_EXPORTED)

            val powerFilter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            registerReceiver(powerSaveReceiver, powerFilter)
        }

        private fun unregisterEngineReceivers() {
            runCatching { unregisterReceiver(updateReceiver) }
            runCatching { unregisterReceiver(powerSaveReceiver) }
        }

        private fun handleError() {
            consecutiveErrors++
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                Log.e(TAG, "Too many consecutive errors, entering safe mode")
                isSafeMode = true
                drawPlaceholder()
            }
        }
    }
}
