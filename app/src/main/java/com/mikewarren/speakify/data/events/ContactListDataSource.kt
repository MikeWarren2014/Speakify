package com.mikewarren.speakify.data.events

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mikewarren.speakify.activities.ContactsFetcherActivity
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.utils.log.ITaggable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContactListDataSource protected constructor(
    context: Context
): BaseDataSource<ContactModel, ContactEvent>(context.applicationContext), ITaggable {

    companion object {
        private var _instance: ContactListDataSource? = null
        fun GetInstance(context: Context) : ContactListDataSource {
            if (_instance == null) {
                _instance = ContactListDataSource(context.applicationContext)
            }

            return _instance!!
        }
    }
    override val eventBus = ContactEventBus.GetInstance()


    private val fetchMutex = Mutex()

    @Volatile
    private var isFetching = false




    init {
        scope.launch {
            eventBus.events().collect { event: ContactEvent ->
                when (event) {
                    is ContactEvent.DataFetched -> {
                        isFetching = false
                        Log.d(TAG, "Contacts fetched successfully. Count: ${event.data.size}")
                        dataFlow.emit(event.data)
                    }

                    is ContactEvent.FetchFailed -> {
                        isFetching = false
                        // Handle error case
                        Log.e(TAG, "Error when fetching the contacts: ${event.message}")
                    }

                    is ContactEvent.PermissionDenied -> {
                        isFetching = false
                        Log.w(TAG, "Permission denied for fetching the contacts")
                    }

                    is ContactEvent.RequestData -> {
                        fetchMutex.withLock {
                            if (!isFetching) {
                                Log.d(TAG, "RequestData event received. Launching ContactsFetcherActivity.")
                                isFetching = true
                                onRequestData()
                                return@collect
                            }
                            Log.d(TAG, "RequestData ignored - Fetch already in progress.")
                        }
                    }
                }
            }
        }
    }

    override fun onRequestData() {
//        context.startActivity(
//            Intent(context, ContactsFetcherActivity::class.java)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
//            null)
        try {
            context.startActivity(
                Intent(context, ContactsFetcherActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ContactsFetcherActivity", e)
            // Optionally emit a failure event back if the activity can't even start
            // scope.launch { eventBus.emit(ContactEvent.FetchFailed("Could not start fetcher activity")) }
        }
    }

    override fun getRequestEvent(): ContactEvent {
        return ContactEvent.RequestData
    }

}