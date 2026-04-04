package com.mikewarren.speakify.data.events

import androidx.datastore.core.Closeable
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseDataRequester<Model, Event : Emittable<Model>> protected constructor(protected val context: Context) : Closeable{
    protected abstract val eventBus: BaseEventBus<Event>
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val dataFlow = MutableStateFlow<List<Model>>(emptyList())

    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun observeData(): MutableStateFlow<List<Model>> {
        return dataFlow
    }

    fun requestData() {
        _isLoading.value = true
        eventBus.post(getRequestEvent())
    }

    abstract fun getRequestEvent() : Event

    abstract fun onRequestData()

    override fun close() {
        scope.cancel()
    }
}
