package com.mikewarren.speakify.strategies

import com.mikewarren.speakify.R
import com.mikewarren.speakify.utils.SearchUtils

interface IThirdPartyMessageNotificationHandler<EnumType>: IMessageNotificationHandler<EnumType> {
    val title: String
    override fun isFromSentMessage(): Boolean {
        if (getMessages().isNotEmpty() && super.isFromSentMessage())
            return true

        return SearchUtils.ContainsKeywords(context.resources.getStringArray(R.array.message_sent_titles),
            title)
    }
}