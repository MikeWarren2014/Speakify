package com.mikewarren.speakify.viewsAndViewModels.widgets

abstract class BaseModelAutoCompletableViewModel<T>:
    BaseAutoCompletableViewModel<T>() {
    override fun filterChoices(searchText: String): List<T> {
        val allChoices = getAllChoices()

        if (allChoices.isEmpty())
            return emptyList()

        return allChoices
            .filter { choiceModel -> 
                toViewString(choiceModel).contains(searchText, true) ||
                toSourceString(choiceModel).contains(searchText, true)
            }
    }

    abstract fun toSourceString(sourceModel: T): String
}
