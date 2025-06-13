package com.mikewarren.speakify.utils

import android.app.Notification
import androidx.core.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.mikewarren.speakify.data.ContactModel

object NotificationExtractionUtils {
    fun ExtractContactModel(sbn: StatusBarNotification, possiblePersonExtras: Array<String>, onPreCheckKey: (StatusBarNotification, String) -> Boolean = ({ _, _ -> true })): ContactModel {
        var contactModel = ContactModel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            contactModel = extractContactFromPeopleList(sbn)
            if (contactModel.phoneNumber != "") {
                return contactModel
            }
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

    @RequiresApi(Build.VERSION_CODES.P)
    fun extractContactFromPeopleList(sbn: StatusBarNotification): ContactModel {
        val peopleBundles: ArrayList<Bundle>? =
            sbn.notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST) // Get ArrayList<Bundle>

        val personBundle = peopleBundles
            ?.firstOrNull()

        if (personBundle == null)
            return ContactModel()

        Person.fromBundle(personBundle)
            .let { person ->
                return ContactModel(
                    -1,
                    person.name.toString(),
                    extractPhoneNumberFromPerson(person),
                )
            }
    }

    private fun extractPhoneNumberFromPerson(person: Person): String {
        if (person.uri == null) return ""
        if (person.uri!!.startsWith("tel:")) {
            return person.uri!!.substringAfter("tel:")
        }
        return ""
    }

    private fun extractPhoneNumberWithLib(text: String?, regionCode: String = "US"): Pair<String, Int> {
        if (text == null) return Pair("", -1)
        val numbersIterator = PhoneNumberUtil.getInstance().findNumbers(text, regionCode)
            .iterator();
        if (numbersIterator.hasNext()) {
            val phoneNumberMatch = numbersIterator.next()
            val number: Phonenumber.PhoneNumber = phoneNumberMatch.number()
            return Pair(PhoneNumberUtil.getInstance()
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