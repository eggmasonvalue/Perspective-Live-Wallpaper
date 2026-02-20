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
import com.timehorizons.wallpaper.data.GridState
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.rendering.CanvasRenderer
import com.timehorizons.wallpaper.rendering.PulseAnimator

/**
 * Base WallpaperService that encapsulates common lifecycle, rendering loop, and scheduling logic.
 * Subclasses need to provide specific state calculation logic.
 */
abstract class BaseWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "BaseWallpaperService"
        private const val MAX_CONSECUTIVE_ERRORS = 5
        private const val PLACEHOLDER_COLOR = 0xFF0A0A0A.toInt()
    }

    override fun onCreateEngine(): Engine {
        return createBaseEngine()
    }

    abstract fun createBaseEngine(): BaseEngine

    abstract inner class BaseEngine : Engine() {

        protected lateinit var preferencesManager: PreferencesManager
        protected var renderer: CanvasRenderer? = null
        protected lateinit var animator: PulseAnimator
        protected lateinit var scheduler: MidnightScheduler
        protected val handler = Handler(Looper.getMainLooper())

        protected var isRenderingVisible: Boolean = false
        protected var surfaceWidth: Int = 0
        protected var surfaceHeight: Int = 0
        private var consecutiveErrors = 0
        protected var isSafeMode: Boolean = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            try {
                super.onCreate(surfaceHolder)

                preferencesManager = PreferencesManager(this@BaseWallpaperService)
                animator = PulseAnimator()
                scheduler = MidnightScheduler(this@BaseWallpaperService)

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
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceCreated", e)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            try {
                super.onSurfaceChanged(holder, format, width, height)
                surfaceWidth = width
                surfaceHeight = height
                initializeRenderer()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceChanged", e)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            try {
                super.onSurfaceDestroyed(holder)
                isRenderingVisible = false
                handler.removeCallbacksAndMessages(null)
                scheduler.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSurfaceDestroyed", e)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            val scheme = renderer?.colorScheme ?: return null
            // Primary: Past/Future Years (Grey in Iconic)
            // Secondary: Current Year/Shape (Red in Iconic - breathing shape)
            // Tertiary: Background (Beige in Iconic)
            return WallpaperColors(
                Color.valueOf(scheme.pastYearsColor),
                Color.valueOf(scheme.currentYearColor),
                Color.valueOf(scheme.backgroundColor)
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            try {
                super.onVisibilityChanged(visible)
                isRenderingVisible = visible

                if (visible) {
                    animator.reset()
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
                try {
                    unregisterReceiver(updateReceiver)
                } catch (e: IllegalArgumentException) {
                    // Ignore if not registered
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onDestroy", e)
            }
        }

        protected open fun initializeRenderer() {
            try {
                if (!hasPreferences()) {
                    drawPlaceholder()
                    return
                }

                val preferences = preferencesManager.getPreferences()
                val gridState = getGridState(preferences) ?: return
                val colorScheme = ColorSchemeProvider.getScheme(preferences.colorSchemeId, preferencesManager)

                renderer = CanvasRenderer(
                    gridState = gridState,
                    colorScheme = colorScheme,
                    screenWidth = surfaceWidth,
                    screenHeight = surfaceHeight
                )

                renderer?.updateStyle(
                    preferences.unitShapeId,
                    preferences.unitScale,
                    preferences.containerPaddingScale
                )

                consecutiveErrors = 0
                isSafeMode = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    notifyColorsChanged()
                }

                drawFrame()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "State invalid", e)
                drawPlaceholder()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing renderer", e)
                handleError()
            }
        }

        protected abstract fun hasPreferences(): Boolean

        protected abstract fun getGridState(preferences: UserPreferences): GridState?

        protected abstract fun performMidnightUpdate(preferences: UserPreferences)

        private fun scheduleNextFrame() {
            if (!isRenderingVisible || isSafeMode) return
            handler.postDelayed({
                drawFrame()
                scheduleNextFrame()
            }, PulseAnimator.FRAME_DURATION_MS)
        }

        private fun drawFrame() {
            if (renderer == null && !isSafeMode) return

            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (isSafeMode) {
                        canvas.drawColor(Color.BLACK)
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

        protected fun drawPlaceholder() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.drawColor(PLACEHOLDER_COLOR)
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing placeholder", e)
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

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MidnightReceiver.ACTION_UPDATE_WALLPAPER) {
                    if (preferencesManager.hasPreferences()) {
                        performMidnightUpdate(preferencesManager.getPreferences())
                        scheduler.scheduleMidnightCheck()
                    }
                }
            }
        }
    }
}
