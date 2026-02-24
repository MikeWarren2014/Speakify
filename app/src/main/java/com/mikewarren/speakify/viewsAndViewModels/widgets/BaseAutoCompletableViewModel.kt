package com.mikewarren.speakify.viewsAndViewModels.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

abstract class BaseAutoCompletableViewModel<T> : ViewModel() {
    var selection by mutableStateOf<T?>(null)
        protected set

    var searchText by mutableStateOf<String>(selection?.let { toViewString(it) } ?: "")
        protected set

    var isAutocompleteDropdownOpen by mutableStateOf(false)
        protected set

    abstract fun getLabelText(): UiText

    abstract fun getAllChoices(): List<T>

    abstract fun filterChoices(searchText: String): List<T>

    abstract fun toViewString(sourceModel: T): String

    fun onSearchTextChanged(newSearchText: String, onCheckSearchText: (String) -> Any) {
        searchText = newSearchText

        onCheckSearchText(newSearchText)

        filterChoices(newSearchText)
    }

    fun setAutocompleteDropdownState(isOpen: Boolean) {
        isAutocompleteDropdownOpen = isOpen
    }
}