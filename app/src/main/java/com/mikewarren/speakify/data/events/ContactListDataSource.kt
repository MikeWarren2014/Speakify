package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mikewarren.speakify.activities.ContactsFetcherActivity
import com.mikewarren.speakify.data.ContactModel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class ContactListDataSource(context: Context): BaseDataSource<ContactModel, ContactEvent>(context) {
    override val eventBus = ContactEventBus.GetInstance()






    init {
        scope.launch {
            eventBus.events().collect { event: ContactEvent ->
                when (event) {
                    is ContactEvent.DataFetched -> {
                        dataFlow.emit(event.data)
                    }

                    is ContactEvent.FetchFailed -> {
                        // Handle error case
                        Log.e(this.javaClass.name, "Error when fetching the contacts")
                    }

                    is ContactEvent.PermissionDenied -> {
                        // TODO: handle the PermissionDenied case
                        Log.w(this.javaClass.name, "Permission denied for fetching the contacts")
                    }

                    is ContactEvent.RequestData -> {
                        onRequestData()
                    }
                }
            }
        }
    }

    override fun onRequestData() {
        context.startActivity(
            Intent(context, ContactsFetcherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            null)
    }

    override fun getRequestEvent(): ContactEvent {
        return ContactEvent.RequestData
    }

}