package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.core.Closeable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.mikewarren.speakify.ContactsFetcherActivity
import com.mikewarren.speakify.data.ContactModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ContactListDataSource(private val context: Context): Closeable {
    private val eventBus = ContactEventBus.GetInstance()
    private val contacts = MutableStateFlow<List<ContactModel>>(emptyList())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun requestContacts() {
        eventBus.post(ContactEvent.RequestContacts)
    }

    fun observeContacts(): MutableStateFlow<List<ContactModel>> {
        return contacts
    }

    init {
        scope.launch {
            eventBus.events().collect { event ->
                when (event) {
                    is ContactEvent.ContactsFetched -> {contacts.emit(event.contacts)
                    }
                    is ContactEvent.FetchFailed -> {
                        // Handle error case
                        Log.e(this.javaClass.name, "Error when fetching the contacts")
                    }

                    ContactEvent.PermissionDenied -> {
                        // TODO: handle the PermissionDenied case
                        Log.w(this.javaClass.name, "Permission denied for fetching the contacts")
                    }
                    ContactEvent.RequestContacts -> {

                        ContextCompat.startActivity(context,
                            Intent(context, ContactsFetcherActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            null)
                    }
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}