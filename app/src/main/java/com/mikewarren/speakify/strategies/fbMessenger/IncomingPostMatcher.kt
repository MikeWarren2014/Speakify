package com.mikewarren.speakify.strategies.fbMessenger

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

object IncomingPostMatcher: BaseIncomingMessageMatcher() {
    override fun getEmojis(): List<String> {
        return listOf("\uD83D\uDDD2", "🗒️")
    }

    override fun getKeywordText(): UiText {
        return UiText.StringResource(R.string.messenger_incoming_post_id_text)
    }

}