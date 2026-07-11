package com.mikewarren.speakify.strategies.fbMessenger

import com.mikewarren.speakify.R
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

object IncomingPhotoMatcher: BaseIncomingMessageMatcher() {
    override fun getEmojis(): List<String> {
        return listOf("\uD83D\uDCF7", "📷", "🖼️")
    }

    override fun getKeywordText(): UiText {
        return UiText.StringResource(R.string.messenger_incoming_photo_id_text)
    }

}