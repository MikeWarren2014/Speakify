package com.mikewarren.speakify.data.events

import androidx.datastore.core.Closeable
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseDataSource<Model, Event : Emittable<Model>> protected constructor(protected val context: Context) : Closeable{
    protected abstract val eventBus: BaseEventBus<Event>
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected val dataFlow = MutableStateFlow<List<Model>>(emptyList())

    fun observeData(): MutableStateFlow<List<Model>> {
        return dataFlow
    }

    fun requestData() {
        eventBus.post(getRequestEvent())
    }

    abstract fun getRequestEvent() : Event

    abstract fun onRequestData()

    override fun close() {
        scope.cancel()
    }
}