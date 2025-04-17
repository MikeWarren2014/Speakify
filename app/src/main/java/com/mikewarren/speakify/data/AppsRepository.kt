package com.mikewarren.speakify.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface AppsRepository {

    val importantApps: StateFlow<List<UserAppModel>> // Now a property
    val otherApps: StateFlow<List<UserAppModel>>     // Now a property

    fun addImportantApp(appModel: UserAppModel)
    fun removeImportantApps(appsToRemove: List<UserAppModel>)
    fun updateApp(app: UserAppModel)
}