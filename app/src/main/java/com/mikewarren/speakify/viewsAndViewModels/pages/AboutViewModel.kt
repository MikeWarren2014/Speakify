package com.mikewarren.speakify.viewsAndViewModels.pages

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AboutInfo(val appName: String, val version: String, val author: String)

class AboutViewModel(private val context: Context) : ViewModel() {
    private val _aboutInfo = MutableStateFlow<AboutInfo?>(null)
    val aboutInfo: StateFlow<AboutInfo?> = _aboutInfo

    init {
        loadAboutInfo()
    }

    private fun loadAboutInfo() {
        val author = "Mike Warren"
        viewModelScope.launch {
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
                val appName = packageInfo.applicationInfo?.loadLabel(packageManager).toString()
                val version = packageInfo.versionName
                _aboutInfo.value = AboutInfo(
                    appName = appName,
                    version = "$appName v$version",
                    author = author
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // Handle the case where package information cannot be found
                _aboutInfo.value = AboutInfo(
                    appName = "Speakify",
                    version = "Speakify v1.0 (fallback)",
                    author = author
                )
            }
        }
    }
}