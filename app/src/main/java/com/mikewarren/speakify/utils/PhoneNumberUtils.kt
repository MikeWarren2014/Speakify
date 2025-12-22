package com.mikewarren.speakify.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber


object PhoneNumberUtils {

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

    public fun ExtractOnlyDigits(text: String?): String {
        if (text == null) return ""
        return text
            .replace("""[^\d+]""".toRegex(), "")
    }

}
