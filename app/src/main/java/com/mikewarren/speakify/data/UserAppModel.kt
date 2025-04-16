package com.mikewarren.speakify.data

data class UserAppModel(
    val packageName: String,
    val appName: String,
    var enabled: Boolean = true,
    var customPrefix: String? = null,
    var rateLimit: Long = 2 * Constants.OneSecond, // milliseconds between notifications
)
