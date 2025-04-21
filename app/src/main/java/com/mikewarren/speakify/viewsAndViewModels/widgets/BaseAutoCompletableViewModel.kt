package com.mikewarren.speakify.viewsAndViewModels.widgets

import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

abstract class BaseAutoCompletableViewModel : ViewModel() {
    var selection by mutableStateOf<String?>(null)
        protected set

    var searchText by mutableStateOf<String>(selection ?: "")
        protected set

    var isAutocompleteDropdownOpen by mutableStateOf(false)
        protected set

    abstract fun getLabel(): String

    abstract fun getAllChoices(): List<String>

    fun filterChoices(searchText: String): List<String> {
        val allChoices = getAllChoices()

        if (allChoices.isEmpty())
            throw IllegalStateException("Cannot filter voices if there are no voices to begin with")

        return allChoices
            .filter { voiceName: String ->
                voiceName.contains(searchText)
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