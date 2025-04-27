package com.mikewarren.speakify.viewsAndViewModels.pages.contactsFetcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ContactsFetcherViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    fun setIsLoading(value : Boolean){
        isLoading = value
    }
}