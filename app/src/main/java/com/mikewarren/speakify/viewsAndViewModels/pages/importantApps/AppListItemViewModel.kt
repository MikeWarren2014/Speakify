package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.data.UserAppModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppListItemViewModel(val model: UserAppModel): ViewModel() {
    var isSelected by mutableStateOf(false)

    fun toggleSelected() {
        this.isSelected = !this.isSelected
    }
}