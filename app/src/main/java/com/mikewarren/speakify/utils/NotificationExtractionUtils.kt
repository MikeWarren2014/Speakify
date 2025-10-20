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
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.mikewarren.speakify.data.ContactModel
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

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

            val phoneNumberMatch = extractPhoneNumberWithLib(text)
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

        return ContactModel(
            -1,
            person.name.toString(),
            extractPhoneNumberFromPerson(context, person),
        )
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("Range")
    private fun extractPhoneNumberFromPerson(context: Context, person: Person): String {
        if (person.uri == null) return ""
        if (person.uri!!.startsWith("tel:")) {
            return person.uri!!.substringAfter("tel:")
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
    private fun getPhoneNumberForContactId(context: Context, contactId: String): String {
        var phoneNumber: String? = null
        val phoneQueryUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(phoneQueryUri, projection, selection, selectionArgs, null)
            // Get the first phone number available for the contact
            if (cursor != null && cursor.moveToFirst()) {
                phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Error querying for phone number", e)
        } finally {
            cursor?.close()
        }
        return phoneNumber ?: ""
    }

    private fun extractPhoneNumberWithLib(text: String?, regionCode: String = "US"): Pair<String, Int> {
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