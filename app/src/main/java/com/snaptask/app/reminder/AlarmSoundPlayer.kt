package com.snaptask.app.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AlarmSoundPlayer {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stop() }

    @Synchronized
    fun playAlarm(context: Context) {
        stop() // Stop any previous sound/vibration

        // 1. Play Loud Alarm Ringtone with USAGE_ALARM
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (_: Exception) {}

        // 2. Play Repeating Vibration Pattern
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vib
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 800, 400, 800, 400, 1000)
                vib?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from start
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(longArrayOf(0, 800, 400, 800, 400, 1000), 0)
            }
        } catch (_: Exception) {}

        // Auto-stop after 15 seconds to prevent infinite ringing
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.postDelayed(autoStopRunnable, 15_000L)
    }

    @Synchronized
    fun stop() {
        mainHandler.removeCallbacks(autoStopRunnable)
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}
    }
}
