package com.timehorizons.wallpaper.rendering

import kotlin.math.PI
import kotlin.math.sin

/**
 * Animates a pulsing/breathing effect for the current year dot.
 * Uses a sine wave for smooth, natural breathing animation.
 */
class PulseAnimator {
    
    private var startTime: Long = System.currentTimeMillis()
    
    companion object {
        /** Minimum opacity during the pulse cycle */
        const val MIN_OPACITY = 0.5f
        
        /** Maximum opacity during the pulse cycle */
        const val MAX_OPACITY = 1.0f
        
        /** Target frames per second */
        const val TARGET_FPS = 45
        
        /** Duration of one frame in milliseconds */
        const val FRAME_DURATION_MS = 1000L / TARGET_FPS
        
        /** Duration of one complete pulse cycle in milliseconds (faster breathing) */
        private const val CYCLE_DURATION_MS = 2000L
    }
    
    /**
     * Gets the current opacity value based on elapsed time.
     * Uses a sine wave to create smooth breathing effect.
     *
     * @return Current opacity value between MIN_OPACITY and MAX_OPACITY
     */
    fun getCurrentOpacity(): Float {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - startTime
        val phase = (elapsed % CYCLE_DURATION_MS).toFloat() / CYCLE_DURATION_MS.toFloat()
        
        // Use sine wave for smooth breathing effect
        val sinValue = sin(phase * 2.0 * PI).toFloat()
        
        // Map from [-1, 1] to [MIN_OPACITY, MAX_OPACITY]
        return MIN_OPACITY + ((sinValue + 1f) / 2f) * (MAX_OPACITY - MIN_OPACITY)
    }
    
    /**
     * Resets the animation to the beginning of the cycle.
     * Call this when the wallpaper becomes visible again.
     */
    fun reset() {
        startTime = System.currentTimeMillis()
    }
}
