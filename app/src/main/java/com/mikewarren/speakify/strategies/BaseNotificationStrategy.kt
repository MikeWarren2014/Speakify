package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import com.mikewarren.speakify.data.AppSettingsModel


abstract class BaseNotificationStrategy(
    val notification: StatusBarNotification,
    val appSettings: AppSettingsModel?,
    val context: Context,
    val tts: TextToSpeech?,
) {

    fun logNotification() {
        Log.d(this.javaClass.name, "================================================");
        Log.d(this.javaClass.name, "Notification POSTED from : ${notification.getPackageName()}")

        // --- Core SBN Details ---
        Log.d(this.javaClass.name, "ID: " + notification.getId());
        Log.d(this.javaClass.name, "Tag: " + notification.getTag());
        Log.d(this.javaClass.name, "isOngoing: " + notification.isOngoing());
        Log.d(this.javaClass.name, "PostTime: " + notification.getPostTime());


        // --- Notification Content Details (from getNotification().getExtras()) ---
        val extras: Bundle = notification.getNotification().extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

        Log.d(this.javaClass.name, "TITLE: ${title ?: "null"}")
        Log.d(this.javaClass.name, "TEXT: ${text ?: "null"}")
        Log.d(this.javaClass.name, "SUB_TEXT: ${subText ?: "null"}")


        // --- Log all Extras for full visibility (Crucial for rich notifications like Messenger/Messages) ---
        Log.d(this.javaClass.name, "--- All Extras Key/Values ---")
        for (key in extras.keySet()) {
            Log.d(this.javaClass.name, "  [${key}]: ${extras.get(key)}")
        }


        // --- Other Key Notification Fields ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(this.javaClass.name, "Channel ID: ${notification.getNotification().getChannelId()}")
        }

        Log.d(this.javaClass.name, "================================================")
    }

    fun speakify() {
        val text: String = textToSpeakify()
        Log.d(this.javaClass.name, "Now speakifying : '${text}'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    abstract fun textToSpeakify() : String
    open fun shouldSpeakify() : Boolean {
        // if the app settings is null or notification sources is empty, we should speakify
        return (appSettings == null || appSettings.notificationSources.isEmpty())
    }

}