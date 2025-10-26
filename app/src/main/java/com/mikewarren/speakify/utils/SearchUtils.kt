package com.mikewarren.speakify.utils

object SearchUtils {
    fun HasAnySubstringOverlap(firstList: List<String>, secondList: List<String>): Boolean{
        return firstList.any { firstItem: String ->
            secondList.any { secondItem: String ->
                firstItem.contains(secondItem, ignoreCase = true) || secondItem.contains(firstItem, ignoreCase = true)
            }
        }
    }

    fun HasAnyOverlap(firstList: List<String>, secondList: List<String>): Boolean {
        return firstList.any { firstItem: String ->
            secondList.any { secondItem: String ->
                firstItem == secondItem
            }
        }
    }

    fun HasAnyMatches(list: List<String>, searchString: String): Boolean{
        return list.any { item: String ->
            item.contains(searchString, ignoreCase = true)
        }
    }

    fun IsInPhoneNumberList(listOfPhoneNumbers: List<String>, phoneNumber: String): Boolean {
        return listOfPhoneNumbers.any { firstPhoneNumber: String ->
            val firstPhoneNumberIntlFormat = NotificationExtractionUtils.ExtractPhoneNumberWithLib(firstPhoneNumber)
                .first
            val phoneNumberIntlFormat = NotificationExtractionUtils.ExtractPhoneNumberWithLib(phoneNumber)
                .first

            return@any firstPhoneNumberIntlFormat == phoneNumberIntlFormat
        }

    }

}