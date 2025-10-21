package com.mikewarren.speakify.viewsAndViewModels.widgets

import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.ViewModel
import java.util.Locale

abstract class BaseAutoCompletableViewModel : ViewModel() {
    var selection by mutableStateOf<String?>(null)
        protected set

    var searchText by mutableStateOf<String>(selection ?: "")
        protected set

    var isAutocompleteDropdownOpen by mutableStateOf(false)
        protected set

    abstract fun getLabel(): String

    abstract fun getAllChoices(): List<String>

    open fun filterChoices(searchText: String): List<String> {
        val allChoices = getAllChoices()

        if (allChoices.isEmpty())
            return emptyList()

        return allChoices
            .filter { choice: String ->
                choice.lowercase().contains(searchText.lowercase())
            }
    }

    fun onSearchTextChanged(newSearchText: String, onCheckSearchText: (String) -> Any) {
        searchText = newSearchText

        onCheckSearchText(newSearchText)

        filterChoices(newSearchText)
    }

    fun setAutocompleteDropdownState(isOpen: Boolean) { // Function to update the flag
        isAutocompleteDropdownOpen = isOpen
    }
}