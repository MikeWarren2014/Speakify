package com.mikewarren.speakify.viewsAndViewModels.widgets

abstract class BaseSimpleAutoCompletableViewModel: BaseAutoCompletableViewModel<String>() {
    override fun filterChoices(searchText: String): List<String> {
        val allChoices = getAllChoices()

        if (allChoices.isEmpty())
            return emptyList()

        return allChoices
            .filter { choice: String ->
                choice.lowercase().contains(searchText.lowercase())
            }
    }

    override fun toViewString(sourceModel: String): String {
        return sourceModel
    }
}
