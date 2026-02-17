package com.timehorizons.wallpaper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.app.WallpaperColors
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import com.timehorizons.wallpaper.data.ColorSchemeProvider
import com.timehorizons.wallpaper.data.LifeState
import com.timehorizons.wallpaper.data.PreferencesManager
import com.timehorizons.wallpaper.data.UserPreferences
import com.timehorizons.wallpaper.modules.DayCounterModule
import com.timehorizons.wallpaper.rendering.CanvasRenderer
import com.timehorizons.wallpaper.rendering.PulseAnimator
import java.time.LocalDate

/**
 * Wallpaper service for the Day Counter feature.
 * Displays a grid of rounded squares representing days until a user-specified event.
 * Reuses the same rendering engine as LifeCalendarService.
 */
class DayCounterService : WallpaperService() {

    companion object {
        private const val TAG = "DayCounterService"
    }
    
    override fun onCreateEngine(): Engine {
        return DayCounterEngine()
    }
    
    /**
     * The wallpaper engine for day counter.
     * Reuses CanvasRenderer, PulseAnimator, and MidnightScheduler.
     */
    inner class DayCounterEngine : Engine() {
        
        private lateinit var preferencesManager: PreferencesManager
        private var renderer: CanvasRenderer? = null
        private lateinit var animator: PulseAnimator
        private lateinit var scheduler: MidnightScheduler
        private val handler = Handler(Looper.getMainLooper())
        private val dayCounterModule = DayCounterModule()
        
        private var isVisible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var consecutiveErrors = 0
        private val MAX_CONSECUTIVE_ERRORS = 5
        private var isSafeMode = false
        
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            try {
                super.onCreate(surfaceHolder)

                
                preferencesManager = PreferencesManager(this@DayCounterService)
                animator = PulseAnimator()
                scheduler = MidnightScheduler(this@DayCounterService)
                
                // Register update receiver
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
         * Initializes the renderer with day counter state.
         */
        private fun initializeRenderer() {
            try {
                if (!preferencesManager.hasDayCounterPreferences()) {

                    drawPlaceholder()
                    return
                }
                
                val preferences = preferencesManager.getPreferences()

                
                // Calculate state using DayCounterModule
                val today = LocalDate.now()
                val totalDays = dayCounterModule.calculateTotalItems(preferences)
                val pastDays = dayCounterModule.calculatePastItems(preferences, today)
                val currentIndex = dayCounterModule.calculateCurrentItemIndex(preferences, today)
                val remainingDays = maxOf(0, totalDays - pastDays - 1)
                
                // Create LifeState compatible structure for CanvasRenderer
                val dayState = LifeState(
                    totalYears = totalDays,
                    yearsLived = pastDays,
                    currentYearIndex = currentIndex,
                    yearsRemaining = remainingDays,
                    birthDate = preferences.countdownStartDate ?: LocalDate.now(),
                    expectedLifespan = totalDays
                )
                
                val colorScheme = ColorSchemeProvider.getScheme(preferences.colorSchemeId, preferencesManager)
                
                renderer = CanvasRenderer(
                    lifeState = dayState,
                    colorScheme = colorScheme,
                    screenWidth = surfaceWidth,
                    screenHeight = surfaceHeight
                )
                
                // Apply style settings
                renderer?.updateStyle(preferences.unitShapeId, preferences.unitScale)
                
                consecutiveErrors = 0
                isSafeMode = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    notifyColorsChanged()
                }
                
                drawFrame()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Day counter onboarding not completed", e)
                drawPlaceholder()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing renderer", e)
                handleError()
            }
        }
        
        private fun drawPlaceholder() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            
            try {
                canvas = holder.lockCanvas()
                canvas?.drawColor(0xFF0A0A0A.toInt())
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
                    // Receiver not registered
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onDestroy", e)
            }
        }
        
        private fun scheduleNextFrame() {
            if (!isVisible || isSafeMode) return
            
            handler.postDelayed({
                drawFrame()
                scheduleNextFrame()
            }, PulseAnimator.FRAME_DURATION_MS)
        }
        
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
         * Called at midnight to update the day counter.
         */
        private fun onMidnight() {
            try {
                if (!preferencesManager.hasDayCounterPreferences()) return
                

                initializeRenderer()
                
                scheduler.scheduleMidnightCheck()
            } catch (e: Exception) {
                Log.e(TAG, "Error in onMidnight", e)
            }
        }
        
        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MidnightReceiver.ACTION_UPDATE_WALLPAPER) {
                    onMidnight()
                }
            }
        }
    }
}
