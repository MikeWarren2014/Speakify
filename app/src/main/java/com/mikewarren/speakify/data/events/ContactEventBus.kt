package com.mikewarren.speakify.data.events

import androidx.datastore.core.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class ContactEventBus private constructor(): Closeable{
    private val events = MutableSharedFlow<ContactEvent>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @OptIn(DelicateCoroutinesApi::class)
    fun post(event: ContactEvent) {
        GlobalScope.launch {
            events.emit(event)
        }
    }

    fun events(): Flow<ContactEvent> = events.asSharedFlow()
        .shareIn(
            scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            replay = 1
        )

    override fun close() {
        scope.cancel()
    }

    companion object {
        private var _instance: ContactEventBus? = null
        fun GetInstance() : ContactEventBus {
            if (_instance == null) {
                _instance = ContactEventBus()
            }

            return _instance!!
        }
    }
}