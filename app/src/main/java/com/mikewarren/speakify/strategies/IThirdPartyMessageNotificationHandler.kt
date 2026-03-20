package com.mikewarren.speakify.strategies

interface IThirdPartyMessageNotificationHandler<EnumType>: IMessageNotificationHandler<EnumType> {
    override fun isFromSentMessage(): Boolean {
        return getMessages().isNotEmpty() && super.isFromSentMessage()
    }
}