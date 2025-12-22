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
            val firstPhoneNumberDigits = PhoneNumberUtils.ExtractOnlyDigits(firstPhoneNumber)
            val phoneNumberDigits = PhoneNumberUtils.ExtractOnlyDigits(phoneNumber)

            return@any firstPhoneNumberDigits == phoneNumberDigits
        }

    }

}