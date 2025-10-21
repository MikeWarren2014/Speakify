package com.mikewarren.speakify.utils

object SearchUtils {
    fun HasMatchesCaseInsensitive(firstList: List<String>, secondList: List<String>): Boolean{
        return firstList.any { firstItem: String ->
            secondList.any { secondItem: String ->
                firstItem.contains(secondItem, ignoreCase = true) || secondItem.contains(firstItem, ignoreCase = true)
            }
        }
    }
}