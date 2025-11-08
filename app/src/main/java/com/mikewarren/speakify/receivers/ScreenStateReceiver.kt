package com.mikewarren.speakify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.util.Log

class ScreenStateReceiver: BroadcastReceiver() {
    private var originalNotificationVolume: Int = -1

    override fun onReceive(context: Context, intent: Intent) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            Log.d("ScreenStateReceiver", "Screen OFF. Maximizing notification volume.")
            // Save the current volume if it hasn't been saved yet
            if (originalNotificationVolume == -1) {
                originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            // Get max volume and set it
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        }

        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("ScreenStateReceiver", "User PRESENT. Restoring original notification volume.")
            // Restore the original volume
            if (originalNotificationVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalNotificationVolume, 0)
                originalNotificationVolume = -1
            }
        }
    }
}