package com.mikewarren.speakify.viewsAndViewModels

import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.viewsAndViewModels.navigation.NavDrawerViewModel

class AppViewModel : ViewModel() {
    val childNavDrawerViewModel = NavDrawerViewModel()

}