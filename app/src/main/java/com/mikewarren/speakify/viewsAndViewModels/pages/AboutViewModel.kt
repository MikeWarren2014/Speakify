package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AboutInfo(val appName: String, val version: String, val author: String)

@HiltViewModel
class AboutViewModel @Inject constructor(
    val aboutInfo: AboutInfo
) : ViewModel()
