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

    public fun IsValidPhoneNumber(phoneNumber: String, regionCode: String = "US"): Boolean{
        if (phoneNumber.isEmpty())
            return false
        if ("""/[A-Za-z]+""".toRegex()
            .find(phoneNumber) != null)
            return false
        return PhoneNumberUtil.getInstance()
            .isPossibleNumber(phoneNumber, regionCode)
    }

    public fun ToI164Format(phoneNumber: String, regionCode: String = "US"): String{
        if (!IsValidPhoneNumber(phoneNumber, regionCode))
            return ""

        return PhoneNumberUtil.getInstance().format(PhoneNumberUtil.getInstance()
            .parse(phoneNumber, regionCode),
            PhoneNumberUtil.PhoneNumberFormat.E164)


    }

}
