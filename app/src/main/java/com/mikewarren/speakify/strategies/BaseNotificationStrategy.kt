package com.mikewarren.speakify.strategies

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.log.ITaggable


abstract class BaseNotificationStrategy(
    val notification: StatusBarNotification,
    val appSettingsModel: AppSettingsModel?,
    val context: Context,
    val ttsManager: TTSManager,
): ITaggable {

    fun logNotification() {

        doLog("================================================");
        doLog("Notification POSTED from : ${notification.getPackageName()}")

        // --- Core SBN Details ---
        doLog("ID: " + notification.getId());
        doLog("Tag: " + notification.getTag());
        doLog("isOngoing: " + notification.isOngoing());
        doLog("PostTime: " + notification.getPostTime());


        // --- Notification Content Details (from getNotification().getExtras()) ---
        val extras: Bundle = notification.getNotification().extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

        doLog("TITLE: ${title ?: "null"}")
        doLog("TEXT: ${text ?: "null"}")
        doLog("SUB_TEXT: ${subText ?: "null"}")


        // --- Log all Extras for full visibility (Crucial for rich notifications like Messenger/Messages) ---
        doLog("--- All Extras Key/Values ---")
        for (key in extras.keySet()) {
            doLog("  [${key}]: ${extras.get(key)}")
        }


        // --- Other Key Notification Fields ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            doLog("Channel ID: ${notification.getNotification().getChannelId()}")
        }

        doLog("================================================")
    }

    protected fun doLog(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
        Log.d(TAG, message)
    }

    suspend fun speakify() {
        val text: String = textToSpeakify()
        // we log this normally (i.e. NOT to Firebase Crashlytics)
        Log.d(TAG, "Now speakifying : '${text}'")
        ttsManager.speak(text, appSettingsModel?.announcerVoice?: Constants.DefaultTTSVoice)
    }
    abstract fun textToSpeakify() : String

    open fun shouldSpeakify() : Boolean {
        // if the app settings is null or notification sources is empty, we should speakify
        return (appSettingsModel == null || appSettingsModel.notificationSources.isEmpty())
    }

}