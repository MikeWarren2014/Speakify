package com.mikewarren.speakify.data

import kotlinx.coroutines.flow.StateFlow

interface AppsRepository {

    val importantApps: StateFlow<List<UserAppModel>>

    fun addImportantApp(appModel: UserAppModel)
    fun removeImportantApps(appsToRemove: List<UserAppModel>)
    fun updateApp(app: UserAppModel)
}