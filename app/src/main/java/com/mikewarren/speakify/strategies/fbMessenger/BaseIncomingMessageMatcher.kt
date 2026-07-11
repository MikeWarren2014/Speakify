package com.mikewarren.speakify.strategies.fbMessenger

import android.content.Context
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

abstract class BaseIncomingMessageMatcher {
    abstract fun getEmojis(): List<String>
    abstract fun getKeywordText(): UiText

    fun checkMessageText(context: Context, text: String): Boolean {
        if (getEmojis().any { text.startsWith(it) })
            return true

        val textParts = text.split(*getEmojis().toTypedArray())

        if (textParts.size > 2)
            return false

        return textParts.any { it.contains(getKeywordText().asString(context), ignoreCase = true) }
    }
}