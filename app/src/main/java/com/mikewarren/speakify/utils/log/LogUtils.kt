package com.mikewarren.speakify.utils.log

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object LogUtils {
    fun LogWarning(tag: String, message: String) {
        Log.w(tag, message)
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun LogNonFatalError(tag: String, message: String, error: Exception) {
        Log.e(tag, message)
        FirebaseCrashlytics.getInstance().log(message)
        FirebaseCrashlytics.getInstance().recordException(error)

    }
}