package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.events.SignInMessageEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMessageRepository @Inject constructor() {
    private val _events = MutableSharedFlow<SignInMessageEvent>()
    val events = _events.asSharedFlow()

    suspend fun postMessage(message: String) {
        _events.emit(SignInMessageEvent(message))
    }
}
