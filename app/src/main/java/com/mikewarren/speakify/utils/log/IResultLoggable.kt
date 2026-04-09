package com.mikewarren.speakify.utils.log

import android.util.Log

interface IResultLoggable: ITaggable {
    fun getSuccessLogMessage() : String
    fun getFailureLogMessage() : String

    fun logSuccessResult() {
        Log.d(TAG, getSuccessLogMessage())
    }

    fun logFailureResult(e: Exception) {
        Log.e(TAG, getFailureLogMessage(), e)
    }
}