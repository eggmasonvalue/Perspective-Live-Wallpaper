package com.timehorizons.wallpaper.service

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import com.timehorizons.wallpaper.data.ColorSchemeProvider
import com.timehorizons.wallpaper.data.LifeState
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.rendering.CanvasRenderer
import com.timehorizons.wallpaper.rendering.PulseAnimator
import com.timehorizons.wallpaper.utils.DateCalculator
import java.time.LocalDate

/**
 * Main wallpaper service entry point for Time Horizons Live Wallpaper.
 * This service is registered in AndroidManifest.xml and creates the wallpaper engine.
 */
class LifeCalendarService : WallpaperService() {

    companion object {
        private const val TAG = "LifeCalendarService"
    }

    override fun onCreateEngine(): Engine {
        return LifeCalendarEngine()
    }

    /**
     * The wallpaper engine that manages lifecycle and rendering of the life calendar.
     * Handles visibility changes, surface changes, animation loop, and midnight updates.
     *
     * Note: Engine is an inner class of WallpaperService, so this must be an inner class too.
     */
    inner class LifeCalendarEngine : Engine() {

        private lateinit var preferencesManager: PreferencesManager
        private var renderer: CanvasRenderer? = null
        private lateinit var animator: PulseAnimator
        private lateinit var scheduler: MidnightScheduler
        private val handler = Handler(Looper.getMainLooper())

        private var isVisible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var consecutiveErrors = 0
        private val MAX_CONSECUTIVE_ERRORS = 5
        private var isSafeMode = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            try {
                super.onCreate(surfaceHolder)


                preferencesManager = PreferencesManager(this@LifeCalendarService)
                animator = PulseAnimator()
                scheduler = MidnightScheduler(this@LifeCalendarService)

                // Register update receiver with RECEIVER_NOT_EXPORTED for security
                val filter = IntentFilter(MidnightReceiver.ACTION_UPDATE_WALLPAPER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(updateReceiver, filter)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onCreate", e)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            try {
                super.onSurfaceCreated(holder)

                // Surface is ready
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceCreated", e)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            try {
                super.onSurfaceChanged(holder, format, width, height)


                surfaceWidth = width
                surfaceHeight = height

                // Recalculate layout with error handling
                initializeRenderer()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceChanged", e)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            try {
                super.onSurfaceDestroyed(holder)
                isVisible = false
                handler.removeCallbacksAndMessages(null)
                scheduler.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceDestroyed", e)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            val scheme = renderer?.colorScheme ?: return null
            return WallpaperColors(Color.valueOf(scheme.backgroundColor), null, null)
        }

        /**
         * Initializes or re-initializes the renderer with current preferences.
         * Draws a placeholder if preferences are not set.
         */
        private fun initializeRenderer() {
            try {
                if (!preferencesManager.hasPreferences()) {

                    drawPlaceholder()
                    return
                }

                val preferences = preferencesManager.getPreferences()


                val lifeState = LifeState.calculate(preferences)
                val colorScheme = ColorSchemeProvider.getScheme(preferences.colorSchemeId, preferencesManager)

                renderer = CanvasRenderer(
                    lifeState = lifeState,
                    colorScheme = colorScheme,
                    screenWidth = surfaceWidth,
                    screenHeight = surfaceHeight
                )

                // Apply style settings
                renderer?.updateStyle(preferences.unitShapeId, preferences.unitScale)

                // Reset error count on successful init
                consecutiveErrors = 0
                isSafeMode = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    // Notify system of color change
                    notifyColorsChanged()
                }

                drawFrame()
            } catch (e: IllegalStateException) {
                // User hasn't completed onboarding
                Log.w(TAG, "Onboarding not completed", e)
                drawPlaceholder()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing renderer", e)
                handleError()
            }
        }

        /**
         * Draws a simple dark background when preferences are not set.
         */
        private fun drawPlaceholder() {
            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                canvas?.let {
                    // Draw simple dark background
                    it.drawColor(0xFF0A0A0A.toInt())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing placeholder", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas in placeholder", e)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            try {
                super.onVisibilityChanged(visible)

                isVisible = visible

                if (visible) {
                    animator.reset()

                    // Re-check preferences in case they changed while invisible
                    if (!isSafeMode) {
                        initializeRenderer()
                    }

                    scheduleNextFrame()
                    scheduler.scheduleMidnightCheck()
                } else {
                    handler.removeCallbacksAndMessages(null)
                    scheduler.cancel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onVisibilityChanged", e)
            }
        }

        override fun onDestroy() {
            try {
                super.onDestroy()

                handler.removeCallbacksAndMessages(null)
                scheduler.cancel()

                // Unregister broadcast receiver
                try {
                    unregisterReceiver(updateReceiver)
                } catch (e: IllegalArgumentException) {
                    // Receiver not registered, ignore
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onDestroy", e)
            }
        }

        /**
         * Schedules the next animation frame.
         */
        private fun scheduleNextFrame() {
            if (!isVisible || isSafeMode) return

            handler.postDelayed({
                drawFrame()
                scheduleNextFrame()
            }, PulseAnimator.FRAME_DURATION_MS)
        }

        /**
         * Draws a single frame of the wallpaper.
         */
        private fun drawFrame() {
            if (renderer == null && !isSafeMode) {
                return
            }

            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (isSafeMode) {
                        canvas.drawColor(Color.BLACK)
                        // Maybe draw an error text if possible
                    } else {
                        val pulseOpacity = animator.getCurrentOpacity()
                        renderer?.render(canvas, pulseOpacity)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in drawFrame", e)
                handleError()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas", e)
                    }
                }
            }
        }

        private fun handleError() {
            consecutiveErrors++
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                Log.e(TAG, "Too many consecutive errors, entering safe mode")
                isSafeMode = true
                drawPlaceholder()
            }
        }

        /**
         * Called when midnight update is triggered.
         * Checks if it's the user's birthday and updates the life state if needed.
         */
        private fun onMidnight() {
            try {
                if (!preferencesManager.hasPreferences()) return

                val preferences = preferencesManager.getPreferences()
                val today = LocalDate.now()

                if (DateCalculator.isBirthdayToday(preferences.birthDate, today)) {

                    // Recalculate life state using modular approach
                    val newLifeState = LifeState.calculate(preferences, today)

                    renderer?.updateLifeState(newLifeState)
                    preferencesManager.updateLastBirthdayCheck(today)
                }

                // Schedule next midnight check
                scheduler.scheduleMidnightCheck()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onMidnight", e)
            }
        }

        /**
         * Broadcast receiver for midnight updates.
         */
        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MidnightReceiver.ACTION_UPDATE_WALLPAPER) {
                    onMidnight()
                }
            }
        }
    }
}
