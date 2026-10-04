package com.mikewarren.speakify.data.events

import android.content.Context
import com.mikewarren.speakify.data.EmailContactModel
import com.mikewarren.speakify.data.db.AppDatabase
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmailContactListDataRequester protected constructor(
    context: Context
) : BaseDataRequester<EmailContactModel, EmailContactEvent>(context.applicationContext), ITaggable {

    companion object {
        private var _instance: EmailContactListDataRequester? = null
        fun GetInstance(context: Context): EmailContactListDataRequester {
            if (_instance == null) {
                _instance = EmailContactListDataRequester(context.applicationContext)
            }
            return _instance!!
        }
    }

    override val eventBus = EmailContactEventBus.GetInstance()
    private val db: AppDatabase = DbProvider.GetDb(context.applicationContext)

    init {
        scope.launch {
            eventBus.events().collect { event ->
                when (event) {
                    is EmailContactEvent.DataFetched -> {
                        dataFlow.emit(event.data)
                    }
                    is EmailContactEvent.FetchFailed -> {
                        // Handle failure
                    }
                    is EmailContactEvent.RequestData -> {
                        onRequestData()
                    }
                }
            }
        }
    }

    override fun onRequestData() {
        scope.launch {
            try {
                val recentContacts = db.recentEmailContactDao().getRecentContacts().first()
                val models = recentContacts.map { EmailContactModel(key = it.key, email = it.email, name = it.name) }
                eventBus.post(EmailContactEvent.DataFetched(models))
            } catch (e: Exception) {
                eventBus.post(EmailContactEvent.FetchFailed(e.message ?: "Unknown error"))
            }
        }
    }

    override fun getRequestEvent(): EmailContactEvent {
        return EmailContactEvent.RequestData
    }
}
