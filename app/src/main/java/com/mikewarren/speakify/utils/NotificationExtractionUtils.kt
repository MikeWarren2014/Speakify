package com.mikewarren.speakify.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Person
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.mikewarren.speakify.data.ContactModel
import java.net.URLDecoder

object NotificationExtractionUtils {
    fun ExtractContactModel(context: Context,
                            sbn: StatusBarNotification,
                            possiblePersonExtras: Array<String>,
                            onPreCheckKey: (StatusBarNotification, String) -> Boolean = ({ _, _ -> true })): ContactModel {
        var contactModel = extractContactFromPeopleList(context, sbn)
        if (contactModel.phoneNumber != "") {
            return contactModel
        }

        possiblePersonExtras.forEach { notificationKey ->
            val text = sbn.notification.extras.getString(notificationKey)
            if ((text == null) || (text == ""))
                return@forEach

            if (!onPreCheckKey(sbn, text))
                return@forEach

            val phoneNumberMatch = ExtractPhoneNumberWithLib(text)
            if (phoneNumberMatch.first.isNotEmpty())
                contactModel = contactModel.copy(phoneNumber = phoneNumberMatch.first)

            var textToSearch = text
            if (phoneNumberMatch.second != -1) {
                textToSearch = text.substring(0, phoneNumberMatch.second)
            }

            if (!isPossibleContactName(textToSearch, phoneNumberMatch))
                return@forEach

            val nameMatchResult = """(?<prefix>Call from |Work |Possible spam: )?(?<contactName>([\w@]+)([ \t][\w@]+)*)"""
                .toRegex()
                .find(textToSearch)

            if (nameMatchResult == null) return@forEach

            val contactNameMatchResult = nameMatchResult.groups["contactName"]
            if (contactNameMatchResult == null) return@forEach
            contactModel = contactModel.copy(name = contactNameMatchResult.value)

            if ((contactModel.name.isNotEmpty()) &&
                (contactModel.phoneNumber.isNotEmpty())
            )
                return contactModel
        }

        return contactModel
    }

    fun extractContactFromPeopleList(context: Context, sbn: StatusBarNotification): ContactModel {
        // TODO: seems we make mistake here in assuming this ArrayList will contain Bundles . It contains Person objects here....
        val personList: ArrayList<Parcelable>? =
            sbn.notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST)

        val person = personList
            ?.firstOrNull() as Person?

        if (person == null)
            return ContactModel()

        var name = person.name?.toString()
        if (name.isNullOrEmpty()) {
            name = extractDisplayNameFromPerson(context, person)
        }

        var phoneNumber = extractPhoneNumberFromPerson(context, person)

        // If we got a name but couldn't find a phone number (because URI was null),
        // we now try to find the phone number using the name.
        if (phoneNumber.isEmpty() && name.isNotEmpty()) {
            phoneNumber = GetPhoneNumberForDisplayName(context, name)
        }

        return ContactModel(
            -1,
            name,
            phoneNumber,
        )
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    private fun extractPhoneNumberFromPerson(context: Context, person: Person): String {
        if (person.uri == null) return ""
        if (person.uri!!.startsWith("tel:")) {
            val encodedPhoneNumber = person.uri!!.substringAfter("tel:")
            return URLDecoder.decode(encodedPhoneNumber, "UTF-8")
        }
        if (person.uri!!.startsWith("content://")) {
            val contactUri = person.uri!!.toUri()
            val contactId = getContactIdFromUri(context, contactUri)

            if (contactId == null) {
                Log.e("NotificationUtils", "Could not find Contact ID for URI: $contactUri")
                return ""
            }

            return getPhoneNumberForContactId(context, contactId)
        }
        return ""
    }

    private fun extractDisplayNameFromPerson(context: Context, person: Person): String {
        if (person.uri == null) return ""

        if (person.uri!!.startsWith("content://")) {
            val contactUri = person.uri!!.toUri()
            val contactId = getContactIdFromUri(context, contactUri)
            if (contactId != null) {
                val name = getNameForContactId(context, contactId)
                if (name.isNotEmpty()) {
                    return name
                }
            }
        }

        if (person.uri!!.startsWith("tel:")) {
            val phoneNumber = URLDecoder.decode(person.uri!!.substringAfter("tel:"), "UTF-8")
            if (phoneNumber.isNotEmpty()) {
                return GetDisplayNameForPhoneNumber(context, phoneNumber)
            }
        }

        return ""
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    private fun getContactIdFromUri(context: Context, contactUri: Uri): String? {
        var contactId: String? = null
        val projection = arrayOf(ContactsContract.Contacts._ID)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(contactUri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error getting contact ID", e)
        } finally {
            cursor?.close()
        }
        return contactId
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    fun GetDisplayNameForPhoneNumber(context: Context, phoneNumber: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var displayName = ""
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error getting display name for phone number", e)
        } finally {
            cursor?.close()
        }
        return displayName
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    public fun GetPhoneNumberForDisplayName(context: Context, displayName: String): String {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        // Search in the Data table where the display name matches and the entry is a phone number.
        val selection = "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(displayName, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        var phoneNumber = ""
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                // Return the first phone number found for that contact name.
                phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error getting phone number for display name: $displayName", e)
        } finally {
            cursor?.close()
        }
        return phoneNumber
    }

    private fun getPhoneNumberForContactId(context: Context, contactId: String): String {
        return getDataForContactId(context, contactId, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
    }

    private fun getNameForContactId(context: Context, contactId: String): String {
        return getDataForContactId(context, contactId, ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    private fun getDataForContactId(context: Context, contactId: String, dataField: String, mimeType: String): String {
        var data: String? = null
        val dataQueryUri = ContactsContract.Data.CONTENT_URI
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, mimeType)
        val projection = arrayOf(dataField)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(dataQueryUri, projection, selection, selectionArgs, null)
            // Get the first phone number available for the contact
            if (cursor != null && cursor.moveToFirst()) {
                data = cursor.getString(cursor.getColumnIndex(dataField))
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error querying for dataField: $dataField for mimeType: $mimeType", e)
        } finally {
            cursor?.close()
        }
        return data ?: ""
    }

    public fun ExtractPhoneNumberWithLib(text: String?, regionCode: String = "US"): Pair<String, Int> {
        if (text == null) return Pair("", -1)
        val numbersIterator = PhoneNumberUtil.getInstance().findNumbers(text, regionCode)
            .iterator();
        if (numbersIterator.hasNext()) {
            val phoneNumberMatch = numbersIterator.next()
            val number: Phonenumber.PhoneNumber = phoneNumberMatch.number()
            return Pair(
                PhoneNumberUtil.getInstance()
                    .format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
                phoneNumberMatch.start(),
            )
        }
        return Pair("", -1)
    }

    private fun isPossibleContactName(text: String?, existingPhoneNumberSearch: Pair<String, Int>?): Boolean {
        if (text == null) return false

        if ((existingPhoneNumberSearch != null) &&
            (existingPhoneNumberSearch.second == 0) &&
            (text == existingPhoneNumberSearch.first))
            return false

        val lowercasedText = text.lowercase()

        if ((lowercasedText == "no service") ||
            (lowercasedText.contains("sim card")) ||
            (lowercasedText.contains("emergency calls")))
            return false

        return true
    }

}
