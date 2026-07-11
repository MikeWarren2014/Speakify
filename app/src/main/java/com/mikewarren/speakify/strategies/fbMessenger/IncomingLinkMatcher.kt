package com.mikewarren.speakify.strategies.fbMessenger

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

object IncomingLinkMatcher: BaseIncomingMessageMatcher() {
    override fun getEmojis(): List<String> {
        return listOf("\uD83D\uDD17", "🔗", "🌐")
    }

    override fun getKeywordText(): UiText {
        return UiText.StringResource(R.string.messenger_incoming_link_id_text)
    }

}