package com.mikewarren.speakify.viewsAndViewModels.pages.fetcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

abstract class BaseFetcherViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    fun setIsLoading(value : Boolean){
        isLoading = value
    }

    abstract fun getDataNameText() : UiText
}