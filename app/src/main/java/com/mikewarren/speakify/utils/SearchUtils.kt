package com.mikewarren.speakify.utils

object SearchUtils {
    /**
     * Checks if there is any common element between two lists.
     * Uses a Set internally for O(N + M) performance.
     */
    fun HasAnyOverlap(first: List<String>, second: List<String>): Boolean {
        val secondSet = second.toSet()
        return first.any { it in secondSet }
    }

    // Overloads to support Arrays seamlessly
    fun HasAnyOverlap(first: Array<String>, second: List<String>): Boolean = HasAnyOverlap(first.toList(), second)
    fun HasAnyOverlap(first: List<String>, second: Array<String>): Boolean = HasAnyOverlap(first, second.toList())
    fun HasAnyOverlap(first: Array<String>, second: Array<String>): Boolean = HasAnyOverlap(first.toList(), second.toList())

    fun HasAnyMatches(list: List<String>, searchString: String): Boolean{
        return list.any { item: String ->
            item.contains(searchString, ignoreCase = true)
        }
    }

    // Overloads to support Arrays seamlessly
    fun HasAnyMatches(list: Array<String>, searchString: String): Boolean = HasAnyMatches(list.toList(), searchString)

    fun IsInPhoneNumberList(listOfPhoneNumbers: List<String>, phoneNumber: String): Boolean {
        val phoneNumberIntlFormat = PhoneNumberUtils.ExtractPhoneNumberWithLib(phoneNumber)
            .first

        return listOfPhoneNumbers.any { firstPhoneNumber: String ->
            val firstPhoneNumberIntlFormat = PhoneNumberUtils.ExtractPhoneNumberWithLib(firstPhoneNumber)
                .first

            return@any firstPhoneNumberIntlFormat == phoneNumberIntlFormat
        }
    }
}
