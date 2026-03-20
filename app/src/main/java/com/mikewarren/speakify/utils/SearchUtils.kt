package com.mikewarren.speakify.utils

import kotlin.reflect.KClass

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

    fun HasAnyMatchesOf(list: List<String>, searchStrings: List<CharSequence?>?): Boolean {
        if (searchStrings == null) return false
        return searchStrings.any { searchString ->
            searchString?.let { HasAnyMatches(list, it.toString()) } ?: false
        }
    }
    fun HasAnyMatchesOf(list: Array<String>, searchStrings: List<CharSequence?>?): Boolean = HasAnyMatchesOf(list.toList(), searchStrings)

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

    /**
     * Checks if the [state] object is an instance of any of the provided [types].
     */
    fun IsAnyOf(state: Any?, vararg types: KClass<*>): Boolean {
        return types.any { it.java.isInstance(state) }
    }

    /**
     * Checks if the [state] object is an instance of any type in the [list].
     */
    fun IsAnyOf(state: Any?, list: List<KClass<*>>): Boolean {
        return list.any { it.java.isInstance(state) }
    }
}
