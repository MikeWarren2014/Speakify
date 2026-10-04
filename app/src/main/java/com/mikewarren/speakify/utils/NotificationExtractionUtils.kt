package com.mikewarren.speakify.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Person
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.utils.log.ITaggable
import com.mikewarren.speakify.utils.log.LogUtils
import java.net.URLDecoder

object NotificationExtractionUtils: ITaggable {
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

            val phoneNumberMatch = PhoneNumberUtils.ExtractPhoneNumberWithLib(text)
            if (phoneNumberMatch.first.isNotEmpty())
                contactModel = contactModel.copy(phoneNumber = phoneNumberMatch.first)

            var textToSearch = text
            if (phoneNumberMatch.second != -1) {
                textToSearch = text.substring(0, phoneNumberMatch.second)
            }

            if (!isPossibleContactName(textToSearch, phoneNumberMatch)) {
                if ((textToSearch.isNullOrEmpty()) && (contactModel.phoneNumber.isNotEmpty())) {
                    contactModel = contactModel.copy(name = GetDisplayNameForPhoneNumber(context, contactModel.phoneNumber))
                }

                return@forEach
            }

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
        val personList: List<Person>? = ExtractPersonList(sbn)

        val person = personList?.firstOrNull()
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

    fun ExtractPersonList(sbn: StatusBarNotification): List<Person>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sbn.notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            sbn.notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST)
        }
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
                LogUtils.LogWarning(TAG, "Could not find Contact ID for URI: $contactUri")
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
            LogUtils.LogNonFatalError(TAG, "Error getting contact ID", e)
        } finally {
            cursor?.close()
        }
        return contactId
    }

    // TODO: this may be YAGNI, considering that we save the contact names along with their phone numbers
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
            LogUtils.LogNonFatalError(TAG, "Error getting display name for phone number", e)
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
            LogUtils.LogNonFatalError(TAG, "Error getting phone number for display name: $displayName", e)
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
            LogUtils.LogNonFatalError(TAG, "Error querying for dataField: $dataField for mimeType: $mimeType", e)
        } finally {
            cursor?.close()
        }
        return data ?: ""
    }

    private fun isPossibleContactName(text: String?, existingPhoneNumberSearch: Pair<String, Int>?): Boolean {
        if (text.isNullOrEmpty()) return false

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

    @OptIn(UnstableApi::class)
    public fun ExtractMessagesManually(extras: Bundle): List<NotificationCompat.MessagingStyle.Message> {
        try {
            val rawMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (rawMessages.isNullOrEmpty())
                return emptyList()
            // These come in as Bundles, and we can use the Compat class to parse them
            return rawMessages.mapNotNull {
                if (it is Bundle) {
                    try {
                        // Manual extraction since there is no public Bundle constructor
                        val text = it.getCharSequence("text")
                        val time = it.getLong("time")
                        val person: androidx.core.app.Person? = if (it.containsKey("person")) {
                            // Try to get the Person object from the bundle
                            it.getParcelable("person") as? androidx.core.app.Person
                                ?: it.getBundle("person")
                                    ?.let { bundle -> androidx.core.app.Person.fromBundle(bundle) }
                        } else if (it.containsKey("sender")) {
                            // Fallback for older versions that used "sender" CharSequence
                            androidx.core.app.Person.Builder()
                                .setName(it.getCharSequence("sender")).build()
                        } else {
                            null
                        }

                        return@mapNotNull NotificationCompat.MessagingStyle.Message(
                            text,
                            time,
                            person
                        )
                    } catch (e: Exception) {
                        return@mapNotNull null
                    }
                }
                return@mapNotNull null
            }
        } catch (e: Exception) {
            LogUtils.LogNonFatalError(TAG, "Failed to manually parse EXTRA_MESSAGES", e)
        }
        return emptyList()
    }

    public fun ExtractTitle(sbn: StatusBarNotification): String {
        return sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    }

    public fun ExtractText(sbn: StatusBarNotification): String {
        return sbn.notification
            .extras
            .getCharSequence(Notification.EXTRA_TEXT)
            ?.trim()
            ?.toString() ?: ""
    }

    public fun ExtractStringExtra(stringExtra: String, sbn: StatusBarNotification): String {
        return sbn.notification
            .extras
            .getCharSequence(stringExtra)
            ?.trim()
            ?.toString() ?: ""
    }

    /**
     * Strips quoted email reply content (e.g., "On Mon, Sep 28... wrote:", "> ", "-----Original Message-----")
     * from email text across localized languages.
     */
    fun StripEmailQuotedReply(text: String, context: Context? = null): String {
        if (text.isBlank()) return text

        var result = text

        // 1. Standard message quote headers
        val standardDelimiters = listOf(
            "-----Original Message-----",
            "-----Original Message",
            "----- Message d'origine -----",
            "----- Ursprüngliche Nachricht -----",
            "----- Mensaje original -----",
            "----- Mensagem original -----",
            "----- Исходное сообщение -----",
            "----- Початкове повідомлення -----",
            "----- 原始邮件 -----",
            "----------------"
        )
        for (delimiter in standardDelimiters) {
            if (result.contains(delimiter, ignoreCase = true)) {
                result = result.substringBefore(delimiter)
            }
        }

        // 2. Lines starting with > or >>
        val lines = result.lines()
        val firstQuoteIndex = lines.indexOfFirst { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith(">") || trimmed.startsWith("»")
        }
        if (firstQuoteIndex >= 0) {
            result = lines.take(firstQuoteIndex).joinToString("\n")
        }

        // 3. Localized prefixes from resources if context is provided
        if (context != null) {
            try {
                val prefixes = context.resources.getStringArray(R.array.email_reply_header_prefixes)
                for (prefix in prefixes) {
                    if (prefix.isNotBlank() && result.contains(prefix, ignoreCase = true)) {
                        result = result.substringBefore(prefix)
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Regex pattern for "On [Date/Time] ... wrote:" or localized date headers
        val replyHeaderRegex = Regex(
            """(?m)^\s*(>+|»+)?\s*(On\s+\w+|Am\s+\w+|El\s+\w+|Le\s+\w+|Il\s+\w+|Em\s+\w+|\w+,\s+\d+|\d{4}[年/-]\d{1,2}[月/-]\d{1,2}).*?(wrote|schrieb|escribió|a écrit|escreveu|ha scritto|написал|写道|:\s*$)""",
            RegexOption.IGNORE_CASE
        )

        val match = replyHeaderRegex.find(result)
        if (match != null) {
            result = result.substring(0, match.range.first)
        }

        return result.trim()
    }
}
