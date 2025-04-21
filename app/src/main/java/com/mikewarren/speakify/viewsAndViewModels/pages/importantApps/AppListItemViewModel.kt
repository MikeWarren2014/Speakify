package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel

open class AppListItemViewModel(open val model: UserAppModel): ViewModel() {

    var isSelected by mutableStateOf(false)

    fun toggleSelected() {
        this.isSelected = !this.isSelected
    }
}