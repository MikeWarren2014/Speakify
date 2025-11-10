package com.mikewarren.speakify.activities

import android.Manifest
import android.provider.ContactsContract
import androidx.activity.viewModels
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.events.ContactEvent
import com.mikewarren.speakify.data.events.ContactEventBus
import com.mikewarren.speakify.viewsAndViewModels.pages.fetcher.ContactFetcherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsFetcherActivity : BaseFetcherActivity<ContactModel, ContactEvent>(
    eventBus = ContactEventBus.GetInstance(),
    permission = Manifest.permission.READ_CONTACTS,
    permissionRequestCode = 1001,
) {
    override val viewModel: ContactFetcherViewModel by viewModels()

    override fun getPermissionDeniedEvent(): ContactEvent {
        return ContactEvent.PermissionDenied
    }

    override fun getFetchFailedEvent(message: String): ContactEvent {
        return ContactEvent.FetchFailed(message)
    }

    override fun getDataFetchedEvent(data: List<ContactModel>): ContactEvent {
        return ContactEvent.DataFetched(data)
    }


    protected override suspend fun fetchDataFromSystem(): List<ContactModel> {
        val resultSetCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null,
        ) ?: return emptyList()

        return resultSetCursor.use { cursor ->
            val contacts = mutableListOf<ContactModel>()
            withContext(Dispatchers.IO) {
                while (cursor.moveToNext()) {
                    var id: Long = 0
                    var name: String = ""
                    var phoneNumber: String = ""

                    val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
                    val phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    if (idIdx > -1)
                        id = cursor.getLong(idIdx)

                    if (nameIdx > -1)
                        name = cursor.getString(nameIdx)

                    if (phoneNumberIdx > -1)
                        phoneNumber = cursor.getString(phoneNumberIdx)

                    if (contacts.any { it.phoneNumber == phoneNumber })
                        continue

                    contacts.add(
                        ContactModel(
                            id,
                            name,
                            phoneNumber,
                        )
                    )
                }
            }
            contacts
        }
    }



}