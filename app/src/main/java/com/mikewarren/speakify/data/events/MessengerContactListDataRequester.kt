package com.mikewarren.speakify.data.events

import android.content.Context
import com.mikewarren.speakify.data.MessengerContactModel
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MessengerContactListDataRequester protected constructor(
    context: Context
) : BaseDataRequester<MessengerContactModel, MessengerContactEvent>(context.applicationContext), ITaggable {

    companion object {
        private var _instance: MessengerContactListDataRequester? = null
        fun GetInstance(context: Context): MessengerContactListDataRequester {
            if (_instance == null) {
                _instance = MessengerContactListDataRequester(context.applicationContext)
            }
            return _instance!!
        }
    }

    override val eventBus = MessengerContactEventBus.GetInstance()
    private val db: AppDatabase = DbProvider.GetDb(context.applicationContext)

    init {
        scope.launch {
            eventBus.events().collect { event ->
                when (event) {
                    is MessengerContactEvent.DataFetched -> {
                        dataFlow.emit(event.data)
                    }
                    is MessengerContactEvent.FetchFailed -> {
                        // Handle failure
                    }
                    is MessengerContactEvent.RequestData -> {
                        onRequestData()
                    }
                }
            }
        }
    }

    override fun onRequestData() {
        scope.launch {
            try {
                // Fetch from the "Recent" room table
                val recentContacts = db.recentMessengerContactDao().getRecentContacts().first()
                val models = recentContacts.map { MessengerContactModel(it.name) }
                eventBus.post(MessengerContactEvent.DataFetched(models))
            } catch (e: Exception) {
                eventBus.post(MessengerContactEvent.FetchFailed(e.message ?: "Unknown error"))
            }
        }
    }

    override fun getRequestEvent(): MessengerContactEvent {
        return MessengerContactEvent.RequestData
    }
}
