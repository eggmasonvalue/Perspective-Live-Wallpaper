package com.perspectivelive.wallpaper.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.perspectivelive.wallpaper.utils.DateCalculator

/**
 * Scheduler for midnight updates using AlarmManager.
 * Schedules an exact alarm to trigger at the next midnight.
 */
class MidnightScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val pendingIntent: PendingIntent

    init {
        val intent = Intent(context, MidnightReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }

    /**
     * Schedules an alarm for the next midnight.
     */
    fun scheduleMidnightCheck() {
        val nextMidnight = DateCalculator.getNextMidnight()

        // Use standard inexact alarm to avoid permission requirements
        // Updates don't need to be perfectly precise for a wallpaper
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            nextMidnight,
            pendingIntent
        )
    }

    /**
     * Cancels any pending midnight alarm.
     */
    fun cancel() {
        alarmManager.cancel(pendingIntent)
    }
}

/**
 * BroadcastReceiver that handles midnight alarm triggers.
 * Sends an update broadcast to the wallpaper engine.
 */
class MidnightReceiver : BroadcastReceiver() {

    companion object {
        /** Action broadcast when the wallpaper should update */
        const val ACTION_UPDATE_WALLPAPER = "com.perspectivelive.wallpaper.UPDATE_WALLPAPER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Send broadcast to wallpaper service
        val updateIntent = Intent(ACTION_UPDATE_WALLPAPER)
        updateIntent.setPackage(context.packageName)
        context.sendBroadcast(updateIntent)

        // Always reschedule for next midnight
        reschedule(context)
    }

    /**
     * Reschedules the midnight alarm for the next day.
     */
    private fun reschedule(context: Context) {
        val scheduler = MidnightScheduler(context)
        scheduler.scheduleMidnightCheck()
    }
}
