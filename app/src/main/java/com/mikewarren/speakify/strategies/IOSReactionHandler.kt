package com.mikewarren.speakify.strategies

import com.mikewarren.speakify.R
import com.mikewarren.speakify.utils.SearchUtils

interface IOSReactionHandler<EnumType>: IMessageNotificationHandler<EnumType> { 
    override fun isReaction(): Boolean { 
        val text = getLatestMessageText()
        if (text.isEmpty()) return false
        
        return SearchUtils.ContainsKeywords(context.resources.getStringArray(R.array.text_reaction_keywords), text)
    }
}
