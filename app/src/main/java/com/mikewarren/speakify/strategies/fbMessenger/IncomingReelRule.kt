package com.mikewarren.speakify.strategies.fbMessenger

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

class IncomingReelRule: BaseIncomingMessageRule() {
    override fun getEmojis(): List<String> {
        return listOf("\uD83C\uDF9E", "🎞️", "🎬", "📽️", "📹", "📺")
    }

    override fun getKeywordText(): UiText {
        return UiText.StringResource(R.string.messenger_incoming_reel_id_text)
    }

}