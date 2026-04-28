package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

abstract class BaseAutoCompletableViewModel<T> : ViewModel(), IStringConverter<T> {
    var selection by mutableStateOf<T?>(null)
        protected set

    var searchText by mutableStateOf<String>(selection?.let { toViewString(it) } ?: "")
        protected set

    var isAutocompleteDropdownOpen by mutableStateOf(false)
        protected set

    var isDisabled by mutableStateOf(false)
        protected set

    abstract fun getLabelText(): UiText

    abstract fun getAllChoices(): List<T>

    open fun filterChoices(searchText: String): List<T> {
        val allChoices = getAllChoices()

        if (allChoices.isEmpty())
            return emptyList()

        return allChoices
            .filter { choiceModel ->
                toViewString(choiceModel).contains(searchText, true) ||
                        toSourceString(choiceModel).contains(searchText, true)
            }
    }


    fun onSearchTextChanged(newSearchText: String, onCheckSearchText: (String) -> Any) {
        searchText = newSearchText

        onCheckSearchText(newSearchText)

        filterChoices(newSearchText)
    }

    fun setAutocompleteDropdownState(isOpen: Boolean) {
        isAutocompleteDropdownOpen = isOpen
    }
}