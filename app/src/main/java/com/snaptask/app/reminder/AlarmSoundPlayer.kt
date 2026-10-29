package com.snaptask.app.reminder

import android.content.Context
import android.media.Ringtone
import android.os.Vibrator

object AlarmSoundPlayer {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun playAlarm(context: Context) {
        // Audio & vibration are handled directly by the system NotificationChannel
        // (snaptask_channel_alarm_v3 with FLAG_INSISTENT) to survive process death.
    }

    @Synchronized
    fun stop() {
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
